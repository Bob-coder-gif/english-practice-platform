package com.jay.englishpracticeplatform.config;

import com.jay.englishpracticeplatform.interceptor.LoginInterceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer{

    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(new LoginInterceptor())
                //拦截所有路径
                .addPathPatterns("/**")
                // 以下路径不需要登录（白名单）
                .excludePathPatterns(
                        "/",
                        "/login",
                        "/register",
                        "/error",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/vendor/**"
                );
    }
}
