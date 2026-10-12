package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.security.AuthUser;
import com.jay.englishpracticeplatform.security.ChangelogPopupListener;
import com.jay.englishpracticeplatform.service.ChangelogService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

//为所有页面统一提供导航栏需要的数据
@ControllerAdvice
public class GlobalModelAttributes {

    private final ChangelogService changelogService;

    public GlobalModelAttributes(ChangelogService changelogService) {
        this.changelogService = changelogService;
    }

    @ModelAttribute
    public void addCommonAttributes(HttpServletRequest request,
                                    @AuthenticationPrincipal AuthUser loginUser,
                                    Model model) {

        model.addAttribute("currentPath", request.getRequestURI());
        if (loginUser != null) {
            model.addAttribute("loginUser", loginUser);
            addChangelogPopup(request, model);
        }
    }

    // 登录后的第一个页面：取出要弹出的更新，取出后立刻删除，保证只弹一次
    // 只在 GET 请求时取：POST 请求处理完通常会重定向，页面并不显示，取出来就浪费了
    private void addChangelogPopup(HttpServletRequest request, Model model) {
        if (!"GET".equals(request.getMethod())) {
            return;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        Object id = session.getAttribute(ChangelogPopupListener.SESSION_KEY);
        if (id instanceof Integer changelogId) {
            session.removeAttribute(ChangelogPopupListener.SESSION_KEY);
            changelogService.findById(changelogId)
                    .ifPresent(entry -> model.addAttribute("popupChangelog", entry));
        }
    }
}