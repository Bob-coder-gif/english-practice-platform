package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.common.SessionKeys;
import com.jay.englishpracticeplatform.dto.LoginUser;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.service.StudyService;
import com.jay.englishpracticeplatform.service.WordService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class StudyController {

    private final StudyService studyService;
    private final WordService wordService;

    public StudyController(StudyService studyService, WordService wordService){
        this.studyService = studyService;
        this.wordService = wordService;
    }

    @GetMapping("/study")
    public String study(@SessionAttribute(SessionKeys.LOGIN_USER) LoginUser loginUser,
                        @RequestParam(defaultValue = "CET4") WordLevel level,
                        Model model){
        model.addAttribute("levels",WordLevel.values());
        model.addAttribute("currentLevel", level);
        model.addAttribute("learnedCount", studyService.countLearned(loginUser.id(),level));
        model.addAttribute("totalCount", wordService.countByLevel(level));

        // 有下一个新单词才放入Model ，全部学完时页面上就没有word
        studyService.nextNewWord(loginUser.id(),level)
                .ifPresent(word -> model.addAttribute("word",word));

        return "study";
    }

    @PostMapping("/study/answer")
    public String answer(@SessionAttribute(SessionKeys.LOGIN_USER) LoginUser loginUser,
                         @RequestParam Long wordId,
                         @RequestParam boolean known,
                         @RequestParam WordLevel level){
        studyService.recordAnswer(loginUser.id(), wordId, known);
        return "redirect:/study?level=" + level.name();
    }
}
