package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.exception.UsernameAlreadyExistsException;
import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.service.UserService;
import com.jay.englishpracticeplatform.exception.InvalidCredentialsException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerSuccess(){
        User user = userService.register("alice","123456");

        assertNotNull(user.getId());
        assertEquals("alice",user.getUsername());
        // 数据库里不是明文密码
        assertNotEquals("123456",user.getPasswordHash());
        // 使用原密码能够校验通过
        assertTrue(passwordEncoder.matches("123456", user.getPasswordHash()));
    }

    @Test
    void registerDuplicateUsernameThrows(){
        userService.register("bob", "123456");

        assertThrows(UsernameAlreadyExistsException.class,
                () -> userService.register("bob", "654321"));
    }

    @Test
    void loginSuccess(){
        userService.register("carol","123456");

        User user = userService.login("carol","123456");

        assertEquals("carol", user.getUsername());
    }

    @Test
    void loginWrongPasswordThrows(){
        userService.register("dava","123456");

        assertThrows(InvalidCredentialsException.class,
                        () -> userService.login("dava" ,"wrong_password"));
    }

    @Test
    void loginNonexistentUserThrows(){
        assertThrows(InvalidCredentialsException.class,
                () -> userService.login("nobody","123456"));
    }
}
