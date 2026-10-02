package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.common.SessionKeys;
import com.jay.englishpracticeplatform.dto.LoginUser;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

@Controller
public class StudyController {

    @GetMapping("/study")
public String study(@SessionAttribute(SessionKeys.LOGIN_USER) LoginUser loginUser,
                    Model model){

        model.addAttribute("loginUser",loginUser);
        return "study";
    }
}
