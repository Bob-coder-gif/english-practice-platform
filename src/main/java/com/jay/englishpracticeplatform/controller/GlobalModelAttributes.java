package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.security.AuthUser;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

//为所有页面统一提供导航栏需要的数据
@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute
    public void addCommonAttributes(HttpServletRequest request,
                                    @AuthenticationPrincipal AuthUser loginUser,
                                    Model model){

        model.addAttribute("currentPath", request.getRequestURI());
        if(loginUser != null){
            model.addAttribute("loginUser",loginUser);
        }
    }
}
