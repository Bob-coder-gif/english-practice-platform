package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.dto.ChangePasswordForm;
import com.jay.englishpracticeplatform.dto.ChangeUsernameForm;
import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.exception.AccountUpdateException;
import com.jay.englishpracticeplatform.security.AuthUser;
import com.jay.englishpracticeplatform.security.CurrentUserRefresher;
import com.jay.englishpracticeplatform.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

// 账号设置：修改用户名、修改密码
// 成功后重定向回个人主页（PRG 模式），失败时回到表单页显示错误
@Controller
@RequestMapping("/account")
public class AccountController {

    private final AccountService accountService;
    private final CurrentUserRefresher currentUserRefresher;

    public AccountController(AccountService accountService, CurrentUserRefresher currentUserRefresher) {
        this.accountService = accountService;
        this.currentUserRefresher = currentUserRefresher;
    }

    // ==================== 修改用户名 ====================

    @GetMapping("/username")
    public String showChangeUsername(@AuthenticationPrincipal AuthUser loginUser, Model model) {
        model.addAttribute("changeUsernameForm", new ChangeUsernameForm());
        addUsernamePageInfo(loginUser, model);
        return "account-username";
    }

    @PostMapping("/username")
    public String changeUsername(@AuthenticationPrincipal AuthUser loginUser,
                                 @Valid @ModelAttribute ChangeUsernameForm changeUsernameForm,
                                 BindingResult bindingResult,
                                 Model model,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                User user = accountService.changeUsername(
                        loginUser.getId(),
                        changeUsernameForm.getNewUsername(),
                        changeUsernameForm.getCurrentPassword(),
                        LocalDateTime.now());

                // 数据库已经改了，Session 里的登录信息也要换成新用户名
                currentUserRefresher.refresh(user, request, response);

                redirectAttributes.addFlashAttribute("message", "用户名已修改为 " + user.getUsername());
                return "redirect:/profile";
            } catch (AccountUpdateException e) {
                bindingResult.rejectValue(e.getField(), "invalid", e.getMessage());
            }
        }
        addUsernamePageInfo(loginUser, model);
        return "account-username";
    }

    // 页面上要显示的辅助信息：当前用户名、冷却天数、下次可修改时间
    private void addUsernamePageInfo(AuthUser loginUser, Model model) {
        model.addAttribute("currentUsername", loginUser.getUsername());
        model.addAttribute("cooldownDays", User.USERNAME_CHANGE_COOLDOWN_DAYS);
        model.addAttribute("nextChangeAt",
                accountService.nextUsernameChangeAt(loginUser.getId(), LocalDateTime.now()));
    }

    // ==================== 修改密码 ====================

    @GetMapping("/password")
    public String showChangePassword(Model model) {
        model.addAttribute("changePasswordForm", new ChangePasswordForm());
        return "account-password";
    }

    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal AuthUser loginUser,
                                 @Valid @ModelAttribute ChangePasswordForm changePasswordForm,
                                 BindingResult bindingResult,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {

        // 两次输入的新密码是否一致（和注册时的写法一样）
        if (changePasswordForm.getNewPassword() != null
                && !changePasswordForm.getNewPassword().equals(changePasswordForm.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "两次输入的新密码不一致");
        }
        if (bindingResult.hasErrors()) {
            return "account-password";
        }

        try {
            accountService.changePassword(
                    loginUser.getId(),
                    changePasswordForm.getCurrentPassword(),
                    changePasswordForm.getNewPassword());
        } catch (AccountUpdateException e) {
            bindingResult.rejectValue(e.getField(), "invalid", e.getMessage());
            return "account-password";
        }

        // 更换 Session ID：如果旧的 Session ID 已经泄露，拿着它的人会因此失去登录状态
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        redirectAttributes.addFlashAttribute("message", "密码已修改，其他设备需要重新登录");
        return "redirect:/profile";
    }
}