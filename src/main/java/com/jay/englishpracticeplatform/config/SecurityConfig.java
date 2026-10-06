package com.jay.englishpracticeplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ① 访问规则：白名单之外的所有请求都需要登录
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/login", "/register", "/error",
                                "/css/**", "/js/**", "/images/**", "/vendor/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // ② 登录：使用我们自己的登录页
                .formLogin(form -> form
                        .loginPage("/login")                // GET /login：显示登录页（由 AuthController 处理）
                        .loginProcessingUrl("/login")       // POST /login：由 Spring Security 处理登录
                        .defaultSuccessUrl("/", false)      // 登录成功：回到登录前想访问的页面，没有就去首页
                        .failureUrl("/login?error")         // 登录失败
                        .permitAll()
                )

                // ③ 退出
                .logout(logout -> logout
                        .logoutUrl("/logout")               // POST /logout
                        .logoutSuccessUrl("/?logout")       // 退出后去首页
                        .permitAll()
                );

        return http.build();
    }
}