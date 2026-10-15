package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.entity.AnswerMode;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.security.AuthUser;
import com.jay.englishpracticeplatform.service.CollectionService;
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
    private final CollectionService collectionService;

    public StudyController(StudyService studyService,
                           WordService wordService,
                           CollectionService collectionService) {
        this.studyService = studyService;
        this.wordService = wordService;
        this.collectionService = collectionService;
    }

    // collection=true：学习「我的词库」中的单词；否则按级别学习
    @GetMapping("/study")
    public String study(@AuthenticationPrincipal AuthUser loginUser,
                        @RequestParam(defaultValue = "CET4") WordLevel level,
                        @RequestParam(defaultValue = "false") boolean collection,
                        Model model) {
        Long userId = loginUser.getId();
        model.addAttribute("levels", WordLevel.values());
        model.addAttribute("currentLevel", level);
        model.addAttribute("collection", collection);

        if (collection) {
            model.addAttribute("learnedCount", collectionService.countLearned(userId));
            model.addAttribute("totalCount", collectionService.count(userId));
            collectionService.nextNewWord(userId)
                    .ifPresent(word -> model.addAttribute("word", word));
        } else {
            model.addAttribute("learnedCount", studyService.countLearned(userId, level));
            model.addAttribute("totalCount", wordService.countByLevel(level));
            // 有下一个新单词才放入 Model，全部学完时页面上就没有 word
            studyService.nextNewWord(userId, level)
                    .ifPresent(word -> model.addAttribute("word", word));
        }
        return "study";
    }

    @PostMapping("/study/answer")
    public String answer(@AuthenticationPrincipal AuthUser loginUser,
                         @RequestParam Long wordId,
                         @RequestParam boolean known,
                         @RequestParam WordLevel level,
                         @RequestParam(defaultValue = "false") boolean collection) {
        studyService.recordAnswer(loginUser.getId(), wordId, known, AnswerMode.LEARN);
        return collection
                ? "redirect:/study?collection=true"
                : "redirect:/study?level=" + level.name();
    }
}