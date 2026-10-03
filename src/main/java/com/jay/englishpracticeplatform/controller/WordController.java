package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.service.WordService;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WordController {

    private final WordService wordService;

    public WordController(WordService wordService){
        this.wordService = wordService;
    }

    @GetMapping("/words")
    public String list(@RequestParam(defaultValue = "CET4") WordLevel level,
                       @RequestParam(defaultValue = "1") int page,
                       Model model){

        // 网址的页码从1开始， Spring Data 的页码从0开始。这里转换
        Page<Word> wordPage = wordService.listByLevel(level, page - 1);

        int totalPages = Math.max(wordPage.getTotalPages(), 1);
        int validPage = Math.min(Math.max(page, 1), totalPages);

        if(validPage != page){
            return "redirect:/words?level=" + level.name() + "&page=" + validPage;
        }

        model.addAttribute("wordPage", wordPage);
        model.addAttribute("currentLevel", level);
        model.addAttribute("levels", WordLevel.values());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        return "words";
    }
}
