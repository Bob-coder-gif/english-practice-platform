package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.service.WordService;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WordController {

    private final WordService wordService;

    public WordController(WordService wordService) {
        this.wordService = wordService;
    }

    // level 不传：全部级别；q 不传：不搜索
    @GetMapping("/words")
    public String list(@RequestParam(required = false) WordLevel level,
                       @RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "1") int page,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        String keyword = WordService.normalizeKeyword(q);
        // 网址的页码从1开始， Spring Data 的页码从0开始。这里转换
        Page<Word> wordPage = wordService.listWords(level, keyword, page - 1);

        int totalPages = Math.max(wordPage.getTotalPages(), 1);
        int validPage = Math.min(Math.max(page, 1), totalPages);

        if (validPage != page) {
            if (level != null) {
                redirectAttributes.addAttribute("level", level.name());
            }
            if (!keyword.isEmpty()) {
                redirectAttributes.addAttribute("q", keyword);
            }
            redirectAttributes.addAttribute("page", validPage);
            return "redirect:/words";
        }

        model.addAttribute("wordPage", wordPage);
        model.addAttribute("currentLevel", level);
        model.addAttribute("levels", WordLevel.values());
        model.addAttribute("q", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        return "words";
    }
}
