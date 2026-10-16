# 英语练习打卡平台

一个基于 **Spring Boot** 的英语单词学习网站：背单词、间隔复习、错题本、听写、每日打卡、学习统计，以及可以收藏单词的个人词库。

考试词库覆盖 **高考、四级、六级、考研、雅思** 五个级别，共 8674 个单词；另有约 **9.6 万**个单词和短语的扩展词典，可以收藏到「我的词库」中学习。

**在线访问：http://122.51.247.76**

> 目前使用 HTTP 访问，暂未启用 HTTPS（域名备案中），注册时请不要使用在其他网站用过的密码。


| 学习 | 听写 | 打卡日历 |
|---|---|---|
| ![学习](docs/images/study.png) | ![听写](docs/images/dictation.png) | ![打卡](docs/images/checkin.png) |


---

## 功能

| 模块 | 说明 |
|---|---|
| **学习新单词** | 按级别或「我的词库」学习，支持「看英文想中文 / 看中文想英文」两种模式，进入新单词时自动朗读 |
| **间隔复习** | 根据记忆情况安排复习时间（1、2、4、7、15、30 天），答错则从头开始 |
| **错题本** | 在复习页的「错题本」标签中；答错过的单词自动进入，复习时连续答对 3 次后自动移出 |
| **听写** | 听写英语（看释义写拼写）、听写汉语（看单词四选一）；单词可来自全部单词、已学单词、错题本或「我的词库」；先预习再答题，题目顺序打乱；可用同一组单词重做或只练错题 |
| **我的词库** | 搜索并收藏单词，每人最多 600 个；除考试词库外，还可以从约 9.6 万个单词和短语的扩展词典中收藏（带「扩展」标记）；收藏的单词可以单独学习和听写 |
| **每日打卡** | 每天答题达到目标自动打卡（目标可自定义），记录连续天数、最长连续天数，月度热力图日历 |
| **学习统计** | 今日各模式答题数、累计正确率、各级别进度、近 7 / 30 天答题柱状图 |
| **单词列表与搜索** | 按级别浏览，支持英文和中文搜索，以搜索词开头的结果排在前面，匹配文字高亮显示 |
| **账号** | 注册、登录、退出、「记住我」、个人主页；修改用户名（需验证密码，3 天内只能改一次）、修改密码 |
| **更新日志** | 记录每次版本更新，登录后弹出最近一次更新（同一条最多提示 3 次），不登录也可以查看 |

---

## 技术栈

| 分类 | 技术 |
|---|---|
| 语言 | Java 21 |
| 框架 | Spring Boot 4、Spring MVC、Spring Security 7 |
| 持久层 | Spring Data JPA（Hibernate 7）、JdbcClient、Flyway |
| 数据库 | MySQL（开发 8.4，线上 8.0） |
| 页面 | Thymeleaf、Bootstrap 5.3、Chart.js 4、原生 JavaScript |
| 测试 | JUnit 6、Spring Boot Test、MockMvc |
| 部署 | Ubuntu 24.04、Nginx、systemd |
| 其他 | Lombok、Jackson 3、Maven、Git |

---

## 项目结构

```
src/main/java/com/jay/englishpracticeplatform
├── config/        配置：Spring Security、密码加密
├── controller/    控制器：接收请求、组织页面数据
├── dto/           数据传输对象：页面展示用的数据、表单
├── entity/        JPA 实体
├── exception/     业务异常
├── init/          启动时导入数据：考试词库、扩展词典
├── repository/    数据访问
├── security/      登录相关：用户加载、「记住我」令牌存储、登录事件监听、刷新登录状态
└── service/       业务逻辑

src/main/resources
├── db/migration/  Flyway 数据库迁移脚本（表结构的唯一来源）
├── templates/     Thymeleaf 页面
├── static/        CSS、JavaScript、第三方前端库
├── vocabulary/    考试词库数据（words.json）
├── dictionary/    扩展词典数据（dict_entries.tsv.gz）
└── changelog.json 更新日志，随代码一起发布

scripts/           词库提取、扩展词典生成、表结构导出清理脚本（Python）；索引性能对比脚本（PowerShell）
docs/images/       README 截图
```

---

## 数据库设计

| 表 | 说明 |
|---|---|
| `users` | 用户；密码以 BCrypt 哈希存储；用户名区分大小写；记录上次修改用户名的时间、更新日志的提示次数 |
| `words` | 单词：拼写（唯一）、音标、释义、来源（`CORE` 考试词库 / `EXTENDED` 从扩展词典收藏进来的） |
| `word_levels` | 单词所属的级别（一个单词可以属于多个级别） |
| `user_words` | 每个用户对每个单词的**当前学习状态**：认识 / 不认识次数、连续认识次数、下次复习时间 |
| `answer_records` | **每一次答题**的流水记录：模式、对错、时间；统计和打卡的数据来源 |
| `check_ins` | 打卡记录，每人每天最多一条 |
| `user_collections` | 我的词库：用户收藏了哪些单词，`(user_id, word_id)` 联合唯一 |
| `dict_entries` | 扩展词典：约 9.6 万个单词和短语，只用于搜索，收藏时才复制到 `words` |
| `persistent_logins` | 「记住我」令牌（结构由 Spring Security 规定） |

`user_words` 和 `answer_records` 的关系类似银行的**余额**和**交易流水**：前者只保存最新状态，后者保留完整的历史，两者在同一个事务中更新。

---

## 设计要点

### 学习与数据

- **间隔重复**：复习间隔按连续答对次数递增，答错后清零重来；状态只能通过 `markKnown` / `markUnknown` 修改，计算逻辑与当前时间解耦，便于单元测试
- **答题流水表**：每次答题在同一个事务中更新学习状态、写入答题记录、检查打卡，保证三者一致
- **打卡与历史解耦**：打卡结果单独存储，修改每日目标不会改写过去的打卡记录；日历颜色按固定档位计算，不随目标变化
- **听写无状态**：题目由单词 id 生成，服务器不在 Session 中保存试卷；交卷后使用 PRG 模式，刷新结果页不会重复记录

### 我的词库

- **只记录关系，不复制单词**：收藏表只保存「哪个用户收藏了哪个单词」，单词本身仍在 `words` 表中，因此学习、复习、错题本、听写、统计无需改动即可支持收藏的单词，只需在出题时增加「我的词库」这一来源
- **数据只来自词典**：只能收藏词库中已有的单词，不允许用户自行填写释义，表中不存在用户编写的内容，也不会有一个用户的数据被其他用户看到
- **约束与上限**：`(user_id, word_id)` 联合唯一约束防止重复收藏；每人最多 600 个，防止恶意大量添加；取消收藏只删除关系，学习记录保留
- **扩展词典按需入库**：扩展词典单独存放在 `dict_entries` 表，只用于搜索；用户收藏某个词条时，才把它复制到 `words` 表（标记为 `EXTENDED`，不属于任何级别），之后学习、听写等功能无需任何修改即可使用。`words` 表只增加真正被收藏过的词
- **并发收藏同一个新词**：复制到 `words` 时依靠拼写的唯一约束兜底；插入失败的一方在新事务中重新查询，避免可重复读隔离级别下读到旧快照
- **搜索**：英文按前缀匹配，可以使用索引；中文在释义中查找；已在 `words` 表中的词不重复显示
- **数据导入**：`scripts/build_dictionary.py` 从 ECDICT 筛选词条（有考试标签、词频前 5 万、柯林斯星级或牛津 3000 的单词，以及由最常用 2000 词组成的短语），生成约 2.4 MB 的压缩文件；程序首次启动时在一个事务中批量导入（多行 `INSERT`），表中已有数据则跳过

### 账号

- **修改用户名**：需验证当前密码（先验证密码再检查用户名，密码错误时不透露任何信息）；3 天冷却期的规则写在实体中、当前时间作为参数传入，可脱离 Spring 单独测试；唯一索引兜底并发冲突
- **刷新登录状态**：修改用户名后重新构造当前用户并显式保存回 Session（Spring Security 6 起不会自动保存），导航栏和退出登录立即使用新用户名
- **「记住我」令牌处理**：修改用户名时，令牌中的用户名随之更新，其他设备不掉线；修改密码时，删除该用户所有令牌，其他设备的自动登录全部失效，并更换当前 Session ID
- **用户名区分大小写**：`users` 和 `persistent_logins` 两张表的用户名列都改为 `utf8mb4_bin`，避免一个用户退出登录时误删另一个仅大小写不同的用户的令牌

### 更新日志

- **内容随代码发布**：更新日志写在 `changelog.json` 中，与代码一起提交和部署，不需要管理后台；启动时读取并校验格式，内容有误时直接启动失败，而不是等到用户访问时才出错
- **基于事件的登录提示**：监听 Spring Security 的登录成功事件（表单登录和「记住我」自动登录都会触发），登录流程本身不需要知道更新日志的存在；同一条更新最多提示 3 次，发布新更新后重新计数

### 数据库结构管理

- **Flyway 版本化迁移**：表结构全部写在 `db/migration` 的 SQL 脚本中，每次变更新增一个版本，启动时自动执行；已执行的脚本由校验和保护，不可修改
- **Hibernate 只做校验**：`ddl-auto=validate`，启动时检查实体与表结构是否一致，不一致则拒绝启动，不再自动修改表
- **已有数据库平滑接入**：通过 baseline 把现有结构登记为版本 1，新环境则从空库完整执行迁移

### 查询

- **聚合查询**：统计数据使用 `GROUP BY` 和条件计数在数据库中完成，不把明细数据搬到内存里计算
- **避免 N+1 查询**：复习和错题本查询使用 `JOIN FETCH` 一次性加载关联的单词，并单独编写计数查询
- **搜索排序**：在 SQL 中用 `CASE WHEN` 自定义排序，以搜索词开头的结果优先
- **索引设计**：答题记录按 `(user_id, answered_at)` 建立联合索引，打卡判定、按天 / 按模式统计等按时间范围的查询由「取出该用户全部记录再过滤」变为索引范围扫描，实测数据见[性能测试](#性能测试)

### 安全

- **密码**：BCrypt 加盐哈希存储；修改用户名、修改密码等敏感操作需要再次验证当前密码
- **登录**：基于 Spring Security，登录后自动更换 Session ID，防止会话固定攻击；用户不存在和密码错误返回相同提示，防止用户名枚举
- **CSRF**：所有表单自动携带 CSRF 令牌；退出登录使用 POST 请求
- **记住我**：使用持久化令牌方案（系列号 + 令牌），令牌每次使用后更换，可检测令牌被盗用；自行实现 `PersistentTokenRepository`，替代已弃用的官方实现
- **会话**：禁用 URL 会话跟踪，会话 ID 只通过 Cookie 传递，避免出现在网址中
- **输入处理**：所有用户输入在服务端校验；搜索时转义 `LIKE` 通配符；页面输出统一转义，前端高亮使用文本节点构造而非 `innerHTML`，防止 XSS
- **越权**：当前用户身份只从登录状态中获取，不信任请求中提交的用户 id；收藏、取消收藏只能操作自己的词库

---

## 性能测试

验证 `answer_records` 上 `(user_id, answered_at)` 联合索引的实际效果。测试脚本：`scripts/bench-index.ps1`。

### 测试条件

- 独立的压测库 `english_bench`，不使用开发库和线上数据
- 答题流水共 **100 万条**：200 个用户，每人 5000 条，时间随机分布在过去一年内，4 种答题模式随机分布
- 本地 Windows + MySQL 8.4；每条 SQL 先执行一次把数据页读进缓冲池，记录第二次的 `EXPLAIN ANALYZE` 结果
- 接口测试请求学习统计页：每轮预热 5 次 + 正式 50 次，共 3 轮，取 P50

「优化前」为只有 `user_id` 单列索引的状态（外键必须有一个以 `user_id` 开头的索引，所以这才是加联合索引之前的真实状态，而不是全表扫描）。

### SQL 层面

| 查询 | 用途 | 优化前 | 优化后 |
|---|---|---|---|
| `countBetween` | 打卡判定：今日答题数 | 扫描 5000 行，约 31 ms | 扫描 9 行，0.04 ms |
| `countByMode` | 统计页：今日各模式答题数 | 扫描 5000 行，约 22 ms | 扫描 9 行，0.06 ms |
| `countByDay` | 统计页：近 7 天每日答题数 | 扫描 5000 行，约 24 ms | 扫描 98 行，0.2 ms |
| `countByDay` | 打卡日历：本月每日答题数 | 扫描 5000 行，约 34 ms | 扫描 220 行，0.12 ms |
| `summarize` | 统计页：累计答题数和答对数 | 扫描 5000 行 | 扫描 5000 行（无时间条件，用不到联合索引的第二列） |

优化前，MySQL 先按 `user_id` 取出该用户的全部 5000 条记录，逐条回表后再按时间过滤；优化后直接在索引上定位时间范围，且除 `countByMode` 外都只读索引、不回表（覆盖索引）。

毫秒数会随缓冲池状态和机器负载波动（同一执行计划的 `summarize` 在几次测试中从 11 ms 到 35 ms 不等），**扫描行数是稳定、可复现的指标**。

### 接口层面

| 状态 | 第 1 轮 P50 | 第 2 轮 P50 | 第 3 轮 P50 |
|---|---|---|---|
| 优化前 | 82.7 ms | 73.1 ms | 75.4 ms |
| 优化后 | 43.5 ms | 44.5 ms | 44.3 ms |

学习统计页 P50 由约 75 ms 降至约 44 ms。

### 尝试过但未采用：覆盖索引

把索引扩展为 `(user_id, answered_at, mode, correct)` 后，上面 5 条查询全部变为覆盖索引扫描，`summarize` 也不再回表（约 5.6 ms）。但统计页还包含十余条其他查询和页面渲染，接口 P50 三轮分别为 34.7 / 49.9 / 41.4 ms，与现有索引的差异在测量误差范围内。考虑到更宽的索引会增加每次答题写入的维护成本，暂未采用。

### 如何复现

<details>
<summary>展开查看步骤</summary>

**1. 建压测库并授权（root 执行）**

```sql
CREATE DATABASE english_bench DEFAULT CHARSET utf8mb4;
GRANT ALL PRIVILEGES ON english_bench.* TO 'jay'@'localhost';
```

**2. 用压测库启动应用**

新建 `src/main/resources/application-bench.properties`（已被 `.gitignore` 忽略），配置连接 `english_bench`，并加上 `spring.jpa.show-sql=false`。以程序实参 `--spring.profiles.active=bench` 启动，Flyway 会自动建表、导入词库。然后在网页上注册账号 `bench1`。

**3. 造数据（在 `english_bench` 中执行，同一个会话内完成）**

```sql
SET SESSION cte_max_recursion_depth = 1000000;

-- 复制 bench1 的密码哈希，造 bench2 ~ bench200
SET @hash = (SELECT password_hash FROM users WHERE username = 'bench1');
INSERT INTO users (username, password_hash, created_at)
WITH RECURSIVE seq(n) AS (SELECT 2 UNION ALL SELECT n + 1 FROM seq WHERE n < 200)
SELECT CONCAT('bench', n), @hash, NOW(6) FROM seq;

-- 给用户和单词编连续序号（id 可能不连续）
CREATE TEMPORARY TABLE tmp_u AS
SELECT id, ROW_NUMBER() OVER (ORDER BY id) - 1 AS rn FROM users WHERE username LIKE 'bench%';
ALTER TABLE tmp_u ADD PRIMARY KEY (rn);
CREATE TEMPORARY TABLE tmp_w AS
SELECT id, ROW_NUMBER() OVER (ORDER BY id) - 1 AS rn FROM words;
ALTER TABLE tmp_w ADD PRIMARY KEY (rn);
SET @wc = (SELECT COUNT(*) FROM tmp_w);

-- 100 万条答题流水
INSERT INTO answer_records (user_id, word_id, correct, mode, answered_at)
WITH RECURSIVE seq(n) AS (SELECT 0 UNION ALL SELECT n + 1 FROM seq WHERE n < 999999)
SELECT u.id, w.id, RAND() < 0.7,
       ELT(1 + FLOOR(RAND() * 4), 'LEARN', 'REVIEW', 'DICTATION_EN', 'DICTATION_CN'),
       NOW(6) - INTERVAL FLOOR(RAND() * 365 * 24 * 60) MINUTE
FROM seq
JOIN tmp_u u ON u.rn = n % 200
JOIN tmp_w w ON w.rn = (n * 7919) % @wc;

ANALYZE TABLE answer_records;
```

**4. 运行脚本**

```powershell
# 只需一次：保存 MySQL 登录信息（密码加密存储在本机，不写进脚本）
mysql_config_editor set --login-path=bench --host=localhost --user=jay --password

# 浏览器用 bench1 登录后，复制 Cookie 中的 JSESSIONID
powershell -ExecutionPolicy Bypass -File .\scripts\bench-index.ps1 -Cookie "JSESSIONID=..."
```

脚本依次切换三种索引状态，分别执行 SQL 和接口测试，结束（包括中途出错）时自动恢复原索引。完整执行计划保存在 `scripts/bench-result-*.txt`。

</details>

---

## 本地运行

### 环境要求

- JDK 21
- MySQL（开发使用 8.4 版本）

项目自带 Maven Wrapper，无需单独安装 Maven。

### 步骤

**1. 创建数据库**

```sql
CREATE DATABASE english_practice DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**2. 配置数据库账号**

复制 `src/main/resources/application-local.properties.example` 为 `application-local.properties`，填写数据库用户名和密码。该文件已被 `.gitignore` 忽略。

**3. 启动**

```bash
# Windows
mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

首次启动时，Flyway 会执行 `db/migration` 下的迁移脚本建出所有表，随后从 `vocabulary/words.json` 导入考试词库、从 `dictionary/dict_entries.tsv.gz` 导入扩展词典（约几秒）。启动完成后访问 http://localhost:8080 。

### 运行测试

```bash
# Windows
mvnw.cmd test

# macOS / Linux
./mvnw test
```

测试会连接本地数据库，并依赖已导入的考试词库和扩展词典，请先完成一次启动。测试数据在每个测试结束后自动回滚。

---

## 部署

线上环境部署在一台 2 核 4G 的云服务器上（Ubuntu 24.04）：

```
浏览器 ──80──▶ Nginx ──▶ Spring Boot (127.0.0.1:8080) ──▶ MySQL 8.0 (127.0.0.1:3306)
```


| 方面 | 做法 |
|---|---|
| 反向代理 | Nginx 对外监听 80 端口，转发给只监听本机的 Spring Boot；应用通过 `X-Forwarded-*` 请求头获取真实访问信息 |
| 进程管理 | systemd 管理应用进程：开机自启、异常退出自动重启，限制 JVM 堆内存 |
| 配置分离 | `application-prod.properties` 只放与环境相关的非敏感配置；数据库账号密码通过服务器上权限为 600 的环境变量文件注入，不进入代码仓库 |
| 数据库 | 应用使用专用账号，只能从本机登录，只拥有本项目数据库的必要权限；表结构由 Flyway 在空库上自动建立 |
| 网络 | 云防火墙只开放 22、80 端口；MySQL 和应用均只监听 `127.0.0.1` |
| 服务器安全 | 使用普通用户 + sudo 运维，不直接使用 root |
| 备份 | cron 每天凌晨自动导出数据库并压缩，保留最近 7 天；修改表结构前手动备份 |
| 更新发布 | 部署脚本完成拉取代码（失败自动重试）、打包、替换、重启和启动检查；保留上一版本以便回滚 |

---

## 后续计划

- [x] 使用 Flyway 管理数据库结构，`ddl-auto` 改为 `validate`
- [x] Controller 层测试（MockMvc）
- [x] 部署上线
- [x] 修改用户名、修改密码（同时处理「记住我」令牌）
- [x] 更新日志与登录提示
- [x] 我的词库（收藏词库中已有的单词）
- [x] 我的词库：接入 ECDICT 扩展词典，支持收藏词库以外的单词和短语
- [ ] 配置域名和 HTTPS
- [ ] 答题时不刷新页面（同时解决手机浏览器自动朗读被拦截的问题）
- [ ] 「记住我」令牌在数据库中哈希存储
- [ ] 使用 GitHub Actions 实现自动测试和部署

---

## 数据来源与许可

- 考试词库和扩展词典的数据均来自 [ECDICT](https://github.com/skywind3000/ECDICT)，MIT License，许可证见 `src/main/resources/vocabulary/LICENSE-ECDICT.txt`
- 单词发音音频来自有道词典的公开发音地址，加载失败时使用浏览器自带的语音合成
- [Bootstrap](https://getbootstrap.com/)，MIT License
- [Chart.js](https://www.chartjs.org/)，MIT License