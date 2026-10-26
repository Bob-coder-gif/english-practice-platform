// ============================================================
// AI 助手：右下角悬浮球，点击打开悬浮窗
// 所有已登录页面都会加载这个文件；没有悬浮窗的页面（未登录、功能关闭）直接跳过
// ============================================================
(function () {
    const panel = document.getElementById('agentPanel');
    const fab = document.getElementById('agentFab');
    if (!panel || !fab) {
        return;
    }

    const messagesEl = document.getElementById('agentMessages');
    const emptyHint = document.getElementById('agentEmpty');
    const form = document.getElementById('agentForm');
    const input = document.getElementById('agentInput');
    const sendBtn = document.getElementById('agentSend');
    const clearBtn = document.getElementById('agentClear');
    const closeBtn = document.getElementById('agentClose');
    const quotaEl = document.getElementById('agentQuota');

    // POST 请求必须带上 CSRF 令牌，否则 Spring Security 返回 403
    const postHeaders = {
        'Content-Type': 'application/json',
        [panel.dataset.csrfHeader]: panel.dataset.csrfToken
    };

    // 工具名 → 页面上显示的中文名
    const TOOL_LABELS = {
        search_words: '查词',
        get_study_stats: '学习统计',
        get_review_due: '待复习',
        get_wrong_words: '错题本',
        get_checkin_status: '打卡',
        get_current_time: '当前时间'
    };

    // 记住悬浮窗是否打开：换页面后自动恢复。只是个人偏好，读写失败（比如隐私模式）就忽略
    const OPEN_KEY = 'agentPanelOpen';

    let busy = false;
    let historyLoaded = false;

    // ===== 打开 / 关闭 =====

    function rememberOpen(open) {
        try {
            sessionStorage.setItem(OPEN_KEY, open ? '1' : '0');
        } catch (e) {
            // 忽略
        }
    }

    // focus：用户主动点开时把光标放进输入框；换页面自动恢复时不抢焦点
    function openPanel(focus) {
        panel.hidden = false;
        fab.setAttribute('aria-expanded', 'true');
        rememberOpen(true);
        // 第一次打开时才加载历史和额度，不在每个页面一打开就请求
        if (!historyLoaded) {
            historyLoaded = true;
            loadHistory();
            loadQuota();
        }
        if (focus) {
            input.focus();
        }
    }

    function closePanel() {
        panel.hidden = true;
        fab.setAttribute('aria-expanded', 'false');
        rememberOpen(false);
        fab.focus();
    }

    fab.addEventListener('click', () => {
        if (panel.hidden) {
            openPanel(true);
        } else {
            closePanel();
        }
    });
    closeBtn.addEventListener('click', closePanel);

    // 焦点在悬浮窗里时按 Esc 关闭
    panel.addEventListener('keydown', event => {
        if (event.key === 'Escape') {
            closePanel();
        }
    });

    // ===== 渲染 =====

    function label(name) {
        return TOOL_LABELS[name] || name;
    }

    // 创建元素并设置文字。所有文字都用 textContent：
    // 模型的回答和工具结果都可能包含 <script> 之类的内容，必须当作纯文本显示，防止 XSS
    function el(tag, className, text) {
        const node = document.createElement(tag);
        if (className) {
            node.className = className;
        }
        if (text !== undefined) {
            node.textContent = text;
        }
        return node;
    }

    // 工具调用过程：用 <details> 实现点击展开，浏览器自带，不需要写 JS
    function toolDetails(toolCalls) {
        const details = el('details', 'agent-tools');
        const names = [...new Set(toolCalls.map(c => label(c.name)))];
        details.appendChild(el('summary', null,
            '查询了 ' + toolCalls.length + ' 次：' + names.join('、')));

        toolCalls.forEach((call, i) => {
            const item = el('div', 'agent-tool-call');
            item.appendChild(el('div', 'agent-tool-title',
                (i + 1) + '. ' + label(call.name) + '（' + call.name + '）'));
            item.appendChild(el('div', 'agent-tool-label', '参数'));
            item.appendChild(el('pre', null, call.arguments || '{}'));
            item.appendChild(el('div', 'agent-tool-label', '结果'));
            item.appendChild(el('pre', null, call.output || '（无结果）'));
            details.appendChild(item);
        });
        return details;
    }

    // 添加一条消息；有工具调用时，过程显示在回答上方，默认折叠
    function addMessage(role, text, toolCalls) {
        emptyHint.hidden = true;

        const row = el('div', 'agent-msg agent-msg-' + role);
        if (toolCalls && toolCalls.length > 0) {
            row.appendChild(toolDetails(toolCalls));
        }
        row.appendChild(el('div', 'agent-bubble', text));

        messagesEl.appendChild(row);
        messagesEl.scrollTop = messagesEl.scrollHeight;
        return row;
    }

    // 底部显示今天还剩几次；用完时变红
    function showQuota(remaining) {
        quotaEl.textContent = '今日剩余 ' + remaining + ' 次';
        quotaEl.classList.toggle('agent-quota-empty', remaining === 0);
    }

    function setBusy(value) {
        busy = value;
        sendBtn.disabled = value;
        sendBtn.textContent = value ? '…' : '发送';
    }

    // ===== 请求 =====

    // 登录过期时，Spring Security 会把请求重定向到登录页，fetch 会跟着拿到一个 HTML 页面。
    // 遇到这种情况刷新整个页面，让浏览器正常跳转到登录页
    async function request(url, options) {
        const res = await fetch(url, Object.assign({credentials: 'same-origin'}, options));
        const type = res.headers.get('Content-Type') || '';
        if (res.redirected || (res.status !== 204 && !type.includes('application/json'))) {
            location.reload();
            throw new Error('登录状态已失效');
        }
        return res;
    }

    async function send(text) {
        if (busy) {
            return;
        }
        setBusy(true);
        addMessage('user', text);
        const pending = addMessage('assistant', '思考中…');
        pending.classList.add('agent-pending');

        try {
            const res = await request(panel.dataset.chatUrl, {
                method: 'POST',
                headers: postHeaders,
                body: JSON.stringify({message: text})
            });
            const data = await res.json();
            pending.remove();
            if (res.ok) {
                addMessage('assistant', data.reply || '（没有回复内容）', data.toolCalls);
                showQuota(data.remaining);
            } else {
                // 429：今天的额度用完了
                if (res.status === 429) {
                    showQuota(0);
                }
                addMessage('error', data.error || '出错了，请稍后再试');
            }
        } catch (e) {
            pending.remove();
            addMessage('error', '网络错误，请稍后再试');
        } finally {
            setBusy(false);
            input.focus();
        }
    }

    // 显示 Session 中已有的对话（包括每个回答的工具调用过程）
    async function loadHistory() {
        try {
            const res = await request(panel.dataset.historyUrl, {method: 'GET'});
            const lines = await res.json();
            lines.forEach(line => addMessage(line.role, line.content, line.toolCalls));
        } catch (e) {
            // 加载失败不影响继续聊天
        }
    }

    async function loadQuota() {
        try {
            const res = await request(panel.dataset.quotaUrl, {method: 'GET'});
            if (res.ok) {
                showQuota((await res.json()).remaining);
            }
        } catch (e) {
            // 加载失败只是不显示剩余次数
        }
    }

    // ===== 输入 =====

    form.addEventListener('submit', event => {
        event.preventDefault();
        const text = input.value.trim();
        if (!text) {
            return;
        }
        input.value = '';

        const command = COMMANDS[text.toLowerCase()];
        if (command) {
            command();
        } else {
            send(text);
        }
    });

    // Enter 发送，Shift + Enter 换行；输入法正在选字时（isComposing）按 Enter 不发送
    input.addEventListener('keydown', event => {
        if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
            event.preventDefault();
            form.requestSubmit();
        }
    });

    // 示例问题：点击直接发送
    panel.querySelectorAll('[data-prompt]').forEach(button => {
        button.addEventListener('click', () => send(button.textContent.trim()));
    });

    async function clearConversation(){
        if(busy){
            return;
        }
        try{
            await request(panel.dataset.clearUrl,{method: 'POST', headers: postHeaders});
            messagesEl.querySelectorAll('.agent-msg').forEach(node => node.remove());
            emptyHint.hidden = false;
        }
        catch (e){
            addMessage('error', '清空错误，请稍后重试');
        }
    }

    clearBtn.addEventListener('click', clearConversation);

    //本地命令：在浏览器里执行，不发给模型，不扣额度
    const COMMANDS = {
        '/clear' : clearConversation,
        '/help' : () => addMessage('assistant',
            '可用命令：\n/clear 清空对话 \n/help \n\n其他内容会直接发给 AI 助手。')
    }

    // 上一个页面里悬浮窗是打开的：自动恢复，但不抢焦点
    try {
        if (sessionStorage.getItem(OPEN_KEY) === '1') {
            openPanel(false);
        }
    } catch (e) {
        // 忽略
    }
})();