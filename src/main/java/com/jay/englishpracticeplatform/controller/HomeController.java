package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.common.SessionKeys;
import com.jay.englishpracticeplatform.dto.LoginUser;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
public class HomeController {

    @GetMapping("/")
public String index(@SessionAttribute(name = SessionKeys.LOGIN_USER, required = false) LoginUser loginUser,
                    Model model){

        //Model的寿命：只有一次请求，每次请求，Spring都会准备一个新的篮子，请求处理完，页面渲染完，篮子就丢弃了/
        // 往 数据篮子 里放入数据，页面上可以通过名字取出来
        model.addAttribute("title", "英语练习打卡平台");
        model.addAttribute("wordCount", 0);
        model.addAttribute("loginUser", loginUser);

        // 没有 @ResponseBody，所以 "index" 是视图名
        // Spring 回去找 templates/index.html
        return "index";
    }
}
