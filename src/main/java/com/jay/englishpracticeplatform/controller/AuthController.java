package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.dto.RegisterForm;
import com.jay.englishpracticeplatform.service.UserService;
import com.jay.englishpracticeplatform.exception.UsernameAlreadyExistsException;
import jakarta.validation.Valid;
import org.springframework.expression.spel.ast.NullLiteral;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService){
        this.userService = userService;
    }

    // 显示注册页面
    @GetMapping("/register")
    public String showRegisterForm(Model model){
        model.addAttribute("registerForm", new RegisterForm());
        return "register";
    }

    // 处理注册表单提交
    @PostMapping("/register")
    public String register( @Valid @ModelAttribute RegisterForm registerForm,
                            BindingResult bindingResult,
                            RedirectAttributes redirectAttributes){

        // 两覅密码是否一致
        if  (registerForm.getPassword() != null
            && !registerForm.getPassword().equals(registerForm.getConfirmPassword())){

            bindingResult.rejectValue("confirmPassword", "mismatch","两次输入的密码不一致");
        }

        // 有任何校验错误，就回到注册页面显示错误
        if (bindingResult.hasErrors()){
            return "register";
        }

        try {
            userService.register(registerForm.getUsername(), registerForm.getPassword());
        }
        catch (UsernameAlreadyExistsException e){
            bindingResult.rejectValue("username","duplicate","该用户名已被注册");
            return "register";
        }

        // 注册成功，重定向到首页
        redirectAttributes.addFlashAttribute("message","注册成功！");
        return "redirect:/";
    }

}
