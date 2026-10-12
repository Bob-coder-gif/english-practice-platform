package com.jay.englishpracticeplatform.security;

import com.jay.englishpracticeplatform.service.ChangelogService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// 监听登录成功事件：决定这次登录后要不要弹出「最近一次更新」
//
// InteractiveAuthenticationSuccessEvent 在用户「主动」登录成功时发出：
// 输入密码登录、「记住我」自动登录都会触发；而同一个 Session 里的后续请求不会触发
// 用事件而不是直接改登录代码：登录流程不需要知道「更新日志」的存在，两边互不依赖
@Component
public class ChangelogPopupListener {

    // Session 里的标记：要弹出的更新编号，GlobalModelAttributes 取出后会删除
    public static final String SESSION_KEY = "changelogPopupId";

    private final ChangelogService changelogService;

    public ChangelogPopupListener(ChangelogService changelogService) {
        this.changelogService = changelogService;
    }

    @EventListener
    public void onLogin(InteractiveAuthenticationSuccessEvent event) {
        if (!(event.getAuthentication().getPrincipal() instanceof AuthUser user)) {
            return;
        }
        // 事件是在处理请求的过程中同步发出的，可以拿到当前请求
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return;
        }
        changelogService.popupOnLogin(user.getId())
                .ifPresent(entry -> attributes.getRequest().getSession().setAttribute(SESSION_KEY, entry.id()));
    }
}