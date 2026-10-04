// ==================== 发音 ====================
// 页面上任何带 data-speak 属性的元素，点击时朗读属性中的单词
document.addEventListener('click', function (event) {
    const target = event.target.closest('[data-speak]');
    if (!target) {
        return;
    }
    const utterance = new SpeechSynthesisUtterance(target.dataset.speak);
    utterance.lang = 'en-US';
    speechSynthesis.cancel();
    speechSynthesis.speak(utterance);
});

// ==================== 防止重复提交 ====================
// 带 data-submit-once 属性的表单，只允许提交一次
document.querySelectorAll('form[data-submit-once]').forEach(function (form) {
    let submitted = false;
    form.addEventListener('submit', function (event) {
        if (submitted) {
            event.preventDefault();
        }
        submitted = true;
    });
});

// ==================== 单词卡片：看英文想中文 / 看中文想英文 ====================
// 页面上有 data-flashcard 的卡片时才启用
(function () {
    const card = document.querySelector('[data-flashcard]');
    if (!card) {
        return;
    }

    const STORAGE_KEY = 'studyMode';
    const english = card.querySelector('[data-side="en"]');
    const chinese = card.querySelector('[data-side="cn"]');
    const revealButton = card.querySelector('[data-reveal]');
    const modeButtons = document.querySelectorAll('[data-mode]');

    // 读取上次的选择；浏览器禁用存储时会抛异常，此时使用默认值
    let mode = 'show-en';
    try {
        mode = localStorage.getItem(STORAGE_KEY) || 'show-en';
    } catch (e) {
        // 忽略，使用默认模式
    }

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
            try {
                localStorage.setItem(STORAGE_KEY, mode);
            } catch (e) {
                // 存不了也不影响本次使用
            }
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