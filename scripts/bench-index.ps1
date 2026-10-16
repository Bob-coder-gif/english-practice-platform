<#
  answer_records 索引对比测试脚本

  依次测试三种索引状态，每种状态下：
    1. SQL 层面：Q1~Q5 各执行两次 EXPLAIN ANALYZE，取第二次（热缓存）的总耗时和最内层扫描行数
    2. 接口层面：请求统计页，预热 + 正式请求，重复多轮，输出每轮 P50 / P95
  测试结束（或中途出错）都会自动恢复为「优化后」的原始索引。

  前提：
    1. 已执行过一次（只需一次，会提示输入 jay 的密码）：
         mysql_config_editor set --login-path=bench --host=localhost --user=jay --password
    2. 应用以 bench 配置运行中，application-bench.properties 里 spring.jpa.show-sql=false
    3. 浏览器已用 bench1 登录，复制了 JSESSIONID

  用法（在项目根目录）：
    powershell -ExecutionPolicy Bypass -File .\scripts\bench-index.ps1 -Cookie "JSESSIONID=你的值"
#>
param(
    [Parameter(Mandatory = $true)][string]$Cookie,
    [string]$Url = "http://localhost:8080/stats",
    [string]$LoginPath = "bench",
    [string]$Database = "english_bench",
    [string]$BenchUser = "bench1",
    [int]$Rounds = 3,
    [int]$Requests = 50,
    [int]$Warmup = 5
)

$ErrorActionPreference = "Stop"
$Invariant = [Globalization.CultureInfo]::InvariantCulture
$ResultFile = Join-Path $PSScriptRoot ("bench-result-" + (Get-Date -Format "yyyyMMdd-HHmmss") + ".txt")

# ---------------- 三种索引状态 ----------------
# 每个都以 user_id 开头，所以任何时候都至少有一个能满足 user_id 外键
$Indexes = [ordered]@{
    before = @{ Name = "idx_tmp_user";                        Cols = "user_id";                            Label = "优化前   (user_id)" }
    after  = @{ Name = "idx_answer_records_user_answered_at"; Cols = "user_id, answered_at";               Label = "优化后   (user_id, answered_at)" }
    cover  = @{ Name = "idx_tmp_cover";                       Cols = "user_id, answered_at, mode, correct"; Label = "覆盖索引 (user_id, answered_at, mode, correct)" }
}

# ---------------- 被测的 5 条 SQL（与代码中的 JPQL 对应） ----------------
$Queries = [ordered]@{
    "Q1 打卡判定 countBetween" = "SELECT COUNT(*) FROM answer_records WHERE user_id = @uid AND answered_at >= CURDATE() AND answered_at < CURDATE() + INTERVAL 1 DAY"
    "Q2 今日按模式 countByMode" = "SELECT mode, COUNT(*), SUM(CASE WHEN correct THEN 1 ELSE 0 END) FROM answer_records WHERE user_id = @uid AND answered_at >= CURDATE() AND answered_at < CURDATE() + INTERVAL 1 DAY GROUP BY mode"
    "Q3 近7天 countByDay"       = "SELECT DATE(answered_at), COUNT(*) FROM answer_records WHERE user_id = @uid AND answered_at >= CURDATE() - INTERVAL 6 DAY AND answered_at < CURDATE() + INTERVAL 1 DAY GROUP BY DATE(answered_at)"
    "Q4 本月日历 countByDay"    = "SELECT DATE(answered_at), COUNT(*) FROM answer_records WHERE user_id = @uid AND answered_at >= DATE_FORMAT(CURDATE(), '%Y-%m-01') AND answered_at < DATE_FORMAT(CURDATE(), '%Y-%m-01') + INTERVAL 1 MONTH GROUP BY DATE(answered_at)"
    "Q5 累计 summarize"         = "SELECT COUNT(id), SUM(CASE WHEN correct THEN 1 ELSE 0 END) FROM answer_records WHERE user_id = @uid"
}

# ---------------- 工具函数 ----------------
function Write-Log([string]$text) {
    Write-Host $text
    Add-Content -Path $ResultFile -Value $text -Encoding UTF8
}

function Invoke-Sql([string]$sql) {
    # -N 不输出列名，-r 不转义换行，-s 精简输出：EXPLAIN ANALYZE 的执行树会原样输出
    $out = $sql | & mysql.exe "--login-path=$LoginPath" -D $Database -N -r -s
    if ($LASTEXITCODE -ne 0) { throw "SQL 执行失败：$sql" }
    return ($out | Out-String)
}

function Get-ExistingIndexes {
    $sql = "SELECT DISTINCT index_name FROM information_schema.statistics " +
           "WHERE table_schema = '$Database' AND table_name = 'answer_records';"
    return (Invoke-Sql $sql) -split "`r?`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne "" }
}

function Set-IndexState([string]$state) {
    $target = $Indexes[$state]
    # 先加目标索引，再删其他的：顺序不能反，否则外键会因缺少 user_id 开头的索引而拒绝删除
    if ((Get-ExistingIndexes) -notcontains $target.Name) {
        Invoke-Sql "ALTER TABLE answer_records ADD INDEX $($target.Name) ($($target.Cols));" | Out-Null
    }
    foreach ($key in $Indexes.Keys) {
        $name = $Indexes[$key].Name
        if ($name -ne $target.Name -and ((Get-ExistingIndexes) -contains $name)) {
            Invoke-Sql "ALTER TABLE answer_records DROP INDEX $name;" | Out-Null
        }
    }
    Invoke-Sql "ANALYZE TABLE answer_records;" | Out-Null
}

function Measure-Query([string]$query) {
    $sql = "SET @uid = (SELECT id FROM users WHERE username = '$BenchUser'); EXPLAIN ANALYZE $query;"
    Invoke-Sql $sql | Out-Null          # 第一次：把数据页读进缓冲池
    $plan = Invoke-Sql $sql             # 第二次：热缓存，记录这次
    $m = [regex]::Matches($plan, 'actual time=([\d.]+)\.\.([\d.]+) rows=(\d+)')
    return [pscustomobject]@{
        TotalMs     = [double]::Parse($m[0].Groups[2].Value, $Invariant)       # 最外层节点的结束时间 = 总耗时
        ScannedRows = [int]$m[$m.Count - 1].Groups[3].Value                    # 最内层节点的行数 = 实际扫描行数
        Covering    = $plan -match 'Covering index'
        Plan        = $plan
    }
}

function Get-Percentile([double[]]$sorted, [double]$p) {
    return [math]::Round($sorted[[int][math]::Floor(($sorted.Count - 1) * $p)], 1)
}

function Measure-Http {
    $code = & curl.exe -s -o NUL -w "%{http_code}" -b $Cookie $Url
    if ($code -ne "200") {
        throw "统计页返回 $code（302 表示 Cookie 失效：重新登录 bench1 并复制新的 JSESSIONID）"
    }
    $result = @()
    for ($r = 1; $r -le $Rounds; $r++) {
        for ($i = 0; $i -lt $Warmup; $i++) { & curl.exe -s -o NUL -b $Cookie $Url | Out-Null }
        $times = for ($i = 0; $i -lt $Requests; $i++) {
            $s = & curl.exe -s -o NUL -w "%{time_total}" -b $Cookie $Url
            [double]::Parse($s, $Invariant) * 1000
        }
        $sorted = [double[]]($times | Sort-Object)
        $result += [pscustomobject]@{
            Round = $r
            P50   = Get-Percentile $sorted 0.50
            P95   = Get-Percentile $sorted 0.95
        }
    }
    return $result
}

# ---------------- 主流程 ----------------
$sqlSummary  = @()
$httpSummary = @()

Write-Log "测试开始：$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Write-Log "统计页：$Url    每轮 预热 $Warmup 次 + 正式 $Requests 次，共 $Rounds 轮"

try {
    foreach ($state in $Indexes.Keys) {
        $label = $Indexes[$state].Label
        Write-Log ""
        Write-Log "==================== $label ===================="
        Write-Host "切换索引中..."
        Set-IndexState $state

        foreach ($q in $Queries.Keys) {
            Write-Host "执行 $q ..."
            $r = Measure-Query $Queries[$q]
            Add-Content -Path $ResultFile -Value "---- $q ----`n$($r.Plan)" -Encoding UTF8
            $sqlSummary += [pscustomobject]@{
                状态     = $state
                查询     = $q
                扫描行数 = $r.ScannedRows
                耗时ms   = $r.TotalMs
                覆盖索引 = $r.Covering
            }
        }

        Write-Host "接口测试中..."
        foreach ($h in (Measure-Http)) {
            $httpSummary += [pscustomobject]@{
                状态      = $state
                轮次      = $h.Round
                P50_ms    = $h.P50
                P95_ms    = $h.P95
            }
        }
    }
}
finally {
    Write-Host ""
    Write-Host "恢复为优化后的原始索引..."
    Set-IndexState "after"
    Write-Log "当前索引：$((Get-ExistingIndexes) -join ', ')"
}

Write-Log ""
Write-Log "==================== SQL 汇总（第二次执行，热缓存） ===================="
Write-Log ($sqlSummary | Format-Table -AutoSize | Out-String)
Write-Log "==================== 接口汇总 ===================="
Write-Log ($httpSummary | Format-Table -AutoSize | Out-String)
Write-Log "完整执行计划已保存到：$ResultFile"