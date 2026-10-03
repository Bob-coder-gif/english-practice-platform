package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.common.SessionKeys;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

//为所有页面统一提供导航栏需要的数据
@ControllerAdvice
public class GlobalModelAttributes {
    @ModelAttribute
    public void addCommonAttributes(HttpServletRequest request,Model model){
        // 当前请求的路几个，用于导航栏高亮
        model.addAttribute("currentPath", request.getRequestURI());

        //getSession(false) 没有Session时不新建，避免给每一个未登录方可都创建一个空Session
        HttpSession session = request.getSession(false);
        if(session != null){
            Object loginUser = session.getAttribute(SessionKeys.LOGIN_USER);
            if(loginUser != null){
                model.addAttribute("loginUser", loginUser);
            }
        }
    }
}
