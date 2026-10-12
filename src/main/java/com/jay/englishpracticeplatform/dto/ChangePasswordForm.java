package com.jay.englishpracticeplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// 修改密码表单
// 「两次输入是否一致」在 Controller 里检查；「当前密码是否正确」「新旧密码是否相同」在 Service 里检查
@Getter
@Setter
public class ChangePasswordForm {

    @NotBlank(message = "请输入当前密码")
    private String currentPassword;

    // 规则和注册时保持一致
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需要在6-32个字符之间")
    private String newPassword;

    @NotBlank(message = "请再次输入新密码")
    private String confirmPassword;
}