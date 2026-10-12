package com.jay.englishpracticeplatform.config;

import com.jay.englishpracticeplatform.security.AuthUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
public class SecurityConfig {

    private static final int REMEMBER_ME_SECONDS = 14 * 24 * 60 * 60;

    // 登录状态的存储位置：先放在请求属性里（本次请求可见），再保存到 Session（后续请求可见）
    // 这就是 Spring Security 默认使用的组合；声明成 Bean，是为了让修改用户名后刷新登录状态时，
    // 和登录过滤器用的是同一个对象
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository()
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   PersistentTokenRepository tokenRepository,
                                                   AuthUserDetailsService userDetailsService,
                                                   SecurityContextRepository securityContextRepository) throws Exception {
        http
                // 访问规则：白名单之外的所有请求都需要登录
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/login", "/register", "/error",
                                "/css/**", "/js/**", "/images/**", "/vendor/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // 登录状态的存储：使用上面声明的 Bean
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository)
                )

                // 登录：使用我们自己的登录页
                .formLogin(form -> form
                        .loginPage("/login")                // GET /login：显示登录页（由 AuthController 处理）
                        .loginProcessingUrl("/login")       // POST /login：由 Spring Security 处理登录
                        .defaultSuccessUrl("/", false)      // 登录成功：回到登录前想访问的页面，没有就去首页
                        .failureUrl("/login?error")         // 登录失败
                        .permitAll()
                )

                // 记住我：令牌保存在数据库中
                .rememberMe(remember -> remember
                        .tokenRepository(tokenRepository)
                        .userDetailsService(userDetailsService)
                        .tokenValiditySeconds(REMEMBER_ME_SECONDS)
                        .rememberMeCookieName("remember-me")
                )

                // 退出
                .logout(logout -> logout
                        .logoutUrl("/logout")               // POST /logout
                        .logoutSuccessUrl("/?logout")       // 退出后去首页
                        .permitAll()
                );

        return http.build();
    }
}