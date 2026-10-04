package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.common.SessionKeys;
import com.jay.englishpracticeplatform.dto.LoginUser;
import com.jay.englishpracticeplatform.entity.UserWord;
import com.jay.englishpracticeplatform.service.StudyService;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
public class ReviewController {

    private final StudyService studyService;

    public ReviewController(StudyService studyService){
        this.studyService = studyService;
    }

    //===================复习====================

    @GetMapping("/review")
    public String review(@SessionAttribute(SessionKeys.LOGIN_USER) LoginUser loginUser, Model model){
        model.addAttribute("dueCount", studyService.countDue(loginUser.id()));
        studyService.nextDueWord(loginUser.id())
                .ifPresent( userWord -> model.addAttribute("userWord", userWord));
        return "review";
    }

    @PostMapping("/review/answer")
    public String answer(@SessionAttribute(SessionKeys.LOGIN_USER) LoginUser loginUser,
                         @RequestParam Long wordId,
                         @RequestParam boolean known){
        studyService.recordAnswer(loginUser.id(), wordId, known);
        return "redirect:/review";
    }

    // ==================错题本==================

    @GetMapping("/mistakes")
    public String mistakes(@SessionAttribute(SessionKeys.LOGIN_USER) LoginUser loginUser,
                           @RequestParam(defaultValue = "1") int page,
                           Model model){
        Page<UserWord> mistakePage = studyService.listMistakes(loginUser.id(), page - 1);

        int totalPages = Math.max(mistakePage.getTotalPages(),1);
        int validPage = Math.min(Math.max(page , 1), totalPages);
        if(validPage != page){
            return "redirect:/mistakes?page=" + validPage;
        }

        model.addAttribute("mistakePage", mistakePage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages",totalPages);
        model.addAttribute("masteredStreak",StudyService.MASTERED_STREAK);
        return "mistakes";
    }
}
