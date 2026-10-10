// ============================================================
// 公共工具
// ============================================================

// 学习模式：show-en 看英文想中文，show-cn 看中文想英文（学习页和复习页共用）
const STUDY_MODE_KEY = 'studyMode';
const DEFAULT_STUDY_MODE = 'show-en';

// 单词发音音频地址（有道词典，type=2 美式，type=1 英式）
// 注意：这不是官方公开的接口，可能失效，所以保留浏览器语音合成作为备用
const AUDIO_URL = 'https://dict.youdao.com/dictvoice?type=2&audio=';

// 全局只用一个播放器：新的发音会替换正在播放的，连续点击时声音不会叠在一起
const player = new Audio();

// 每次调用 speak 都编号，用来判断失败回调是不是来自最新一次发音
let speakId = 0;

// 用浏览器自带的语音合成朗读；不支持时什么也不做
function speakWithSynthesis(text) {
    if (!('speechSynthesis' in window)) {
        return;
    }
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'en-US';
    speechSynthesis.cancel();
    speechSynthesis.speak(utterance);
}

// 朗读一个英文单词：优先播放真人发音音频，音频加载失败时退回语音合成
function speak(text) {
    if (!text) {
        return;
    }
    const id = ++speakId;
    let fellBack = false;

    // 退回语音合成：同一次发音只退回一次，而且只处理最新一次发音
    function fallback() {
        if (fellBack || id !== speakId) {
            return;
        }
        fellBack = true;
        speakWithSynthesis(text);
    }

    if ('speechSynthesis' in window) {
        speechSynthesis.cancel();
    }
    player.onerror = fallback;
    player.src = AUDIO_URL + encodeURIComponent(text);

    const playing = player.play();
    if (playing !== undefined) {
        playing.catch(function (error) {
            // NotAllowedError：浏览器禁止自动播放，换成语音合成也一样会被禁止，不退回
            // AbortError：被下一次发音打断，属于正常情况
            if (error.name === 'NotAllowedError' || error.name === 'AbortError') {
                return;
            }
            fallback();
        });
    }
}

// 读取学习模式；浏览器禁用存储时会抛异常，此时使用默认值
function readStudyMode() {
    try {
        return localStorage.getItem(STUDY_MODE_KEY) || DEFAULT_STUDY_MODE;
    } catch (e) {
        return DEFAULT_STUDY_MODE;
    }
}

// 保存学习模式；存不了也不影响本次使用
function saveStudyMode(mode) {
    try {
        localStorage.setItem(STUDY_MODE_KEY, mode);
    } catch (e) {
        // 忽略
    }
}

// ============================================================
// 发音按钮：页面上任何带 data-speak 属性的元素，点击时朗读
// ============================================================
document.addEventListener('click', function (event) {
    const target = event.target.closest('[data-speak]');
    if (target) {
        speak(target.dataset.speak);
    }
});

// ============================================================
// 防止重复提交：带 data-submit-once 属性的表单只允许提交一次
// ============================================================
document.querySelectorAll('form[data-submit-once]').forEach(function (form) {
    let submitted = false;
    form.addEventListener('submit', function (event) {
        if (submitted) {
            event.preventDefault();
        }
        submitted = true;
    });
});

// ============================================================
// 单词卡片：看英文想中文 / 看中文想英文（页面上有 data-flashcard 时才启用）
// ============================================================
(function () {
    const card = document.querySelector('[data-flashcard]');
    if (!card) {
        return;
    }

    const english = card.querySelector('[data-side="en"]');
    const chinese = card.querySelector('[data-side="cn"]');
    const revealButton = card.querySelector('[data-reveal]');
    const modeButtons = document.querySelectorAll('[data-mode]');
    let mode = readStudyMode();

    // 根据当前模式，决定显示哪一面
    function render() {
        const showEnglish = (mode === 'show-en');
        english.hidden = !showEnglish;
        chinese.hidden = showEnglish;
        revealButton.hidden = false;
        revealButton.textContent = showEnglish ? '显示释义' : '显示单词';
        modeButtons.forEach(function (button) {
            button.classList.toggle('active', button.dataset.mode === mode);
        });
    }

    // 切换模式
    modeButtons.forEach(function (button) {
        button.addEventListener('click', function () {
            mode = button.dataset.mode;
            saveStudyMode(mode);
            render();
        });
    });

    // 揭晓答案：两面都显示
    revealButton.addEventListener('click', function () {
        english.hidden = false;
        chinese.hidden = false;
        revealButton.hidden = true;
    });

    render();
})();

// ============================================================
// 自动发音：页面加载后朗读带 data-autospeak 的元素
// 「看中文想英文」模式下不朗读单词卡片，否则等于直接说出了答案
// ============================================================
(function () {
    const target = document.querySelector('[data-autospeak]');
    if (!target) {
        return;
    }
    if (target.closest('[data-flashcard]') && readStudyMode() === 'show-cn') {
        return;
    }
    speak(target.dataset.autospeak);
})();

// ============================================================
// 搜索结果高亮：容器上的 data-highlight 是关键词，
// 容器里带 data-highlight-text 的元素，会把匹配的部分用 <mark> 标出来
// 只用 createTextNode 和 textContent 构造内容，不用 innerHTML，避免 XSS
// ============================================================
(function () {
    const container = document.querySelector('[data-highlight]');
    if (!container) {
        return;
    }
    const keyword = container.dataset.highlight.trim();
    if (!keyword) {
        return;
    }
    const lowerKeyword = keyword.toLowerCase();

    container.querySelectorAll('[data-highlight-text]').forEach(function (element) {
        const text = element.textContent;
        const lowerText = text.toLowerCase();
        let index = lowerText.indexOf(lowerKeyword);
        if (index === -1) {
            return;
        }

        // 按「普通文字 - 匹配文字 - 普通文字 ……」的顺序逐段创建节点
        const fragment = document.createDocumentFragment();
        let start = 0;
        while (index !== -1) {
            fragment.appendChild(document.createTextNode(text.slice(start, index)));

            const mark = document.createElement('mark');
            mark.textContent = text.slice(index, index + keyword.length);
            fragment.appendChild(mark);

            start = index + keyword.length;
            index = lowerText.indexOf(lowerKeyword, start);
        }
        fragment.appendChild(document.createTextNode(text.slice(start)));

        element.replaceChildren(fragment);
    });
})();