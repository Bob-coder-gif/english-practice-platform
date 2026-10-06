package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.entity.AnswerMode;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.security.AuthUser;
import com.jay.englishpracticeplatform.service.StudyService;
import com.jay.englishpracticeplatform.service.WordService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class StudyController {

    private final StudyService studyService;
    private final WordService wordService;

    public StudyController(StudyService studyService, WordService wordService){
        this.studyService = studyService;
        this.wordService = wordService;
    }

    @GetMapping("/study")
    public String study(@AuthenticationPrincipal AuthUser loginUser,
                        @RequestParam(defaultValue = "CET4") WordLevel level,
                        Model model){
        model.addAttribute("levels",WordLevel.values());
        model.addAttribute("currentLevel", level);
        model.addAttribute("learnedCount", studyService.countLearned(loginUser.getId(),level));
        model.addAttribute("totalCount", wordService.countByLevel(level));

        // 有下一个新单词才放入Model ，全部学完时页面上就没有word
        studyService.nextNewWord(loginUser.getId(),level)
                .ifPresent(word -> model.addAttribute("word",word));

        return "study";
    }

    @PostMapping("/study/answer")
    public String answer(@AuthenticationPrincipal AuthUser loginUser,
                         @RequestParam Long wordId,
                         @RequestParam boolean known,
                         @RequestParam WordLevel level){
        studyService.recordAnswer(loginUser.getId(), wordId, known, AnswerMode.LEARN);
        return "redirect:/study?level=" + level.name();
    }
}
