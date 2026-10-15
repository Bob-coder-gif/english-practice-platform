package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.entity.AnswerMode;
import com.jay.englishpracticeplatform.entity.UserWord;
import com.jay.englishpracticeplatform.security.AuthUser;
import com.jay.englishpracticeplatform.service.StudyService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class ReviewController {

    // 复习页的两个标签
    static final String TAB_DUE = "due";            // 待复习
    static final String TAB_MISTAKES = "mistakes";  // 错题本

    private final StudyService studyService;

    public ReviewController(StudyService studyService) {
        this.studyService = studyService;
    }

    // 复习页：「待复习」和「错题本」两个标签，用 ?tab= 切换
    @GetMapping("/review")
    public String review(@AuthenticationPrincipal AuthUser loginUser,
                         @RequestParam(defaultValue = TAB_DUE) String tab,
                         @RequestParam(defaultValue = "1") int page,
                         Model model) {
        Long userId = loginUser.getId();

        // 两个标签上都显示数量，所以两个数都要查
        model.addAttribute("dueCount", studyService.countDue(userId));
        model.addAttribute("mistakeCount", studyService.countMistakes(userId));

        if (TAB_MISTAKES.equals(tab)) {
            Page<UserWord> mistakePage = studyService.listMistakes(userId, page - 1);
            int totalPages = Math.max(mistakePage.getTotalPages(), 1);
            int validPage = Math.min(Math.max(page, 1), totalPages);
            if (validPage != page) {
                return "redirect:/review?tab=" + TAB_MISTAKES + "&page=" + validPage;
            }
            model.addAttribute("tab", TAB_MISTAKES);
            model.addAttribute("mistakePage", mistakePage);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("masteredStreak", StudyService.MASTERED_STREAK);
        } else {
            // 其他任何值都当作「待复习」
            model.addAttribute("tab", TAB_DUE);
            studyService.nextDueWord(userId)
                    .ifPresent(userWord -> model.addAttribute("userWord", userWord));
        }
        return "review";
    }

    @PostMapping("/review/answer")
    public String answer(@AuthenticationPrincipal AuthUser loginUser,
                         @RequestParam Long wordId,
                         @RequestParam boolean known) {
        studyService.recordAnswer(loginUser.getId(), wordId, known, AnswerMode.REVIEW);
        return "redirect:/review";
    }

    // 错题本已经合并到复习页。保留旧地址，跳转到新位置，这样以前的书签和链接还能用
    @GetMapping("/mistakes")
    public String mistakes() {
        return "redirect:/review?tab=" + TAB_MISTAKES;
    }
}