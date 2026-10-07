package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.service.WordService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final WordService wordService;

    public HomeController(WordService wordService) {
        this.wordService = wordService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("wordCount", wordService.countAll());
        return "index";
    }
}
