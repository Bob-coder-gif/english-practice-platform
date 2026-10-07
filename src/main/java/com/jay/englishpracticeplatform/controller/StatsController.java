package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.security.AuthUser;
import com.jay.englishpracticeplatform.service.StatsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class StatsController {
    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    public String stats(@AuthenticationPrincipal AuthUser loginUser,
                        @RequestParam(defaultValue = "7") int days,
                        Model model) {
        //只允许7-30 其他值一律按7天处理
        int safeDays = StatsService.ALLOWED_DAYS.contains(days) ? days : StatsService.DEFAULT_DAYS;

        model.addAttribute("stats", statsService.getStats(loginUser.getId(), safeDays));
        model.addAttribute("days", safeDays);

        return "stats";
    }
}
