package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.service.CheckInService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.jay.englishpracticeplatform.security.AuthUser;

import java.time.YearMonth;

@Controller
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @GetMapping("/checkin")
    public String checkIn(@AuthenticationPrincipal AuthUser loginUser,
                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                          Model model) {
        YearMonth validMonth = checkInService.clampMonth(loginUser.getId(), month);
        if (month != null && !month.equals(validMonth)) {
            return "redirect:/checkin?month=" + validMonth;
        }

        model.addAttribute("view", checkInService.getView(loginUser.getId(), validMonth));
        model.addAttribute("minGoal", CheckInService.MIN_DAILY_GOAL);
        model.addAttribute("maxGoal", CheckInService.MAX_DAILY_GOAL);
        return "checkin";
    }

    @PostMapping("/checkin/goal")
    public String updateGoal(@AuthenticationPrincipal AuthUser loginUser,
                             @RequestParam int dailyGoal,
                             RedirectAttributes redirectAttributes) {
        try {
            checkInService.updateDailyGoal(loginUser.getId(), dailyGoal);
            redirectAttributes.addFlashAttribute("message", "每日目标已设置为 " + dailyGoal + " 题");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/checkin";
    }
}