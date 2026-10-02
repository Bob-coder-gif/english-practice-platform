package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.dto.RegisterForm;
import com.jay.englishpracticeplatform.service.UserService;
import com.jay.englishpracticeplatform.exception.UsernameAlreadyExistsException;
import com.jay.englishpracticeplatform.common.SessionKeys;
import com.jay.englishpracticeplatform.dto.LoginForm;
import com.jay.englishpracticeplatform.dto.LoginUser;
import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.exception.InvalidCredentialsException;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServlet;

import org.springframework.expression.spel.ast.NullLiteral;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.InvalidIsolationLevelException;
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


    //================ 注册 =====================
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

    //============== 登录 ====================
    @GetMapping("/login")
    public String showLoginForm(Model model){
        model.addAttribute("loginForm", new LoginForm());
        return "login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginForm loginForm,
                        BindingResult bindingResult,
                        HttpServletRequest request,
                        Model model){

        if(bindingResult.hasErrors()){
            return "login";
        }

        User user;
        try{
            user = userService.login(loginForm.getUsername(), loginForm.getPassword());
        }
        catch (InvalidCredentialsException e){
            model.addAttribute("loginError", e.getMessage());
            return "login";
        }

        // 获取当前 Session （没有创建一个）
        HttpSession session = request.getSession();
        // 登陆成功后更换 Session ID， 防止会话固定攻击
        request.changeSessionId();
        // 在 Session 里记录当前登陆的用户
        session.setAttribute(SessionKeys.LOGIN_USER, new LoginUser(user.getId(), user.getUsername()));

        return "redirect:/";
    }

    //================ 退出 ==================

    @PostMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes){
        //销毁整个Session 里面的所有数据都会被清除
        session.invalidate();
        redirectAttributes.addFlashAttribute("message", "已退出登录");
        return "redirect:/";
    }
}
