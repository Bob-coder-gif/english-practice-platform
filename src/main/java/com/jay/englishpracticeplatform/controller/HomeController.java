package com.jay.englishpracticeplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model){
        // 往 数据篮子 里放入数据，页面上可以通过名字取出来
        model.addAttribute("title","英语练习打卡平台");
        model.addAttribute("wordCount", 0);

        // 没有 @ResponseBody，所以 "index" 是视图名
        // Spring 回去找 templates/index.html
        return "index";
    }
}
