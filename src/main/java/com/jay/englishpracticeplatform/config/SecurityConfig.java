package com.jay.englishpracticeplatform.config;

import com.jay.englishpracticeplatform.security.AuthUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

@Configuration
public class SecurityConfig {

    private static final int REMEBER_NE_SECONDS = 14 * 24 * 60 * 60;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   PersistentTokenRepository tokenRepository,
                                                   AuthUserDetailsService userDetailsService) throws Exception {
        http
                // 访问规则：白名单之外的所有请求都需要登录
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/login", "/register", "/error",
                                "/css/**", "/js/**", "/images/**", "/vendor/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // 登录：使用我们自己的登录页
                .formLogin(form -> form
                        .loginPage("/login")                // GET /login：显示登录页（由 AuthController 处理）
                        .loginProcessingUrl("/login")       // POST /login：由 Spring Security 处理登录
                        .defaultSuccessUrl("/", false)      // 登录成功：回到登录前想访问的页面，没有就去首页
                        .failureUrl("/login?error")         // 登录失败
                        .permitAll()
                )

                //  记住我：令牌保存在数据库中
                .rememberMe(remember -> remember
                        .tokenRepository(tokenRepository)
                        .userDetailsService(userDetailsService)
                        .tokenValiditySeconds(REMEBER_NE_SECONDS)
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