package com.jay.englishpracticeplatform.security;

import com.jay.englishpracticeplatform.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

// 用户信息（比如用户名）修改后，刷新当前登录状态
//
// 登录成功后，代表当前用户的 AuthUser 被保存在 Session 里。
// 数据库改了，Session 里的还是旧对象：导航栏会显示旧用户名，退出登录时也会按旧用户名去删令牌。
// 所以要用新数据重新构造 AuthUser，替换掉 Session 里的认证信息。
@Component
public class CurrentUserRefresher {

    private final SecurityContextRepository securityContextRepository;

    public CurrentUserRefresher(SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    public void refresh(User user, HttpServletRequest request, HttpServletResponse response) {
        // 不保存密码哈希：和登录后 eraseCredentials() 的效果一致
        AuthUser principal = new AuthUser(user.getId(), user.getUsername(), null);

        // 能走到这里，用户刚刚输入过当前密码，所以构造一个「已认证」的令牌
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());

        // 新建一个 SecurityContext，而不是直接修改旧的：旧对象可能正被其他请求共享
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Spring Security 6 以后不会自动把修改保存回 Session，必须显式保存
        securityContextRepository.saveContext(context, request, response);
    }
}