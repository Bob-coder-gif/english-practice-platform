package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.service.ChangelogService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

// 更新日志页面：不需要登录
@Controller
public class ChangelogController {

    private final ChangelogService changelogService;

    public ChangelogController(ChangelogService changelogService) {
        this.changelogService = changelogService;
    }

    @GetMapping("/changelog")
    public String changelog(Model model) {
        model.addAttribute("entries", changelogService.all());
        return "changelog";
    }
}