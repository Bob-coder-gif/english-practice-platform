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