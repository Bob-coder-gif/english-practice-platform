package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.security.AuthUser;
import com.jay.englishpracticeplatform.service.ProfileService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService){
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal AuthUser loginUser,
                          Model model){
        model.addAttribute("profile" , profileService.getProfile(loginUser.getId()));
        return "profile";
    }
}
