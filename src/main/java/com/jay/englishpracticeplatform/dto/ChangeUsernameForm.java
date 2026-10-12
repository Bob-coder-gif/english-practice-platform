package com.jay.englishpracticeplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// 修改用户名表单：修改用户名属于敏感操作，需要验证当前密码
@Getter
@Setter
public class ChangeUsernameForm {

    // 规则和注册时保持一致
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需要在3-20个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母，数字和下划线")
    private String newUsername;

    @NotBlank(message = "请输入当前密码")
    private String currentPassword;
}