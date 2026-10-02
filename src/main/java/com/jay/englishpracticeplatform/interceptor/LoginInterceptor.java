package com.jay.englishpracticeplatform.interceptor;

import com.jay.englishpracticeplatform.common.SessionKeys;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.web.servlet.HandlerInterceptor;

public class LoginInterceptor implements HandlerInterceptor{

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception{

        // getSession(false),（只是检查，没必要为了未登录的访问者在服务器上创建一个空的 Session，浪费内存） 有 Session 就返回， 没有就返回null ，不会新建
        HttpSession session = request.getSession(false);

        if(session != null && session.getAttribute(SessionKeys.LOGIN_USER) != null){
            //已登录 放行
            return true;
        }

        // 未登录， 重定向到登录页，并带上一个参数，让登录页显示提示
        response.sendRedirect(request.getContextPath() + "/login?required");
        return false;   //拦截。 请求不会再到达 Controller
    }
}
