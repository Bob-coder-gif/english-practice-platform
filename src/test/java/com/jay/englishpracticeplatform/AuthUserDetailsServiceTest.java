package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.security.AuthUser;
import com.jay.englishpracticeplatform.security.AuthUserDetailsService;
import com.jay.englishpracticeplatform.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthUserDetailsServiceTest {

    @Autowired
    private AuthUserDetailsService authUserDetailsService;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loadsExistingUser() {
        User user = userService.register("security_tester", "123456");

        UserDetails details = authUserDetailsService.loadUserByUsername("security_tester");

        assertInstanceOf(AuthUser.class, details);
        assertEquals(user.getId(), ((AuthUser) details).getId());
        assertEquals("security_tester", details.getUsername());
        // 返回的是密码哈希，Spring Security 会用它和用户输入的密码比对
        assertTrue(passwordEncoder.matches("123456", details.getPassword()));
    }

    @Test
    void unknownUserThrows() {
        assertThrows(UsernameNotFoundException.class,
                () -> authUserDetailsService.loadUserByUsername("nobody_here"));
    }

    @Test
    void usernameIsCaseSensitive() {
        User upper = userService.register("CaseTester01", "123456");

        // 大小写不同，就是另一个用户名：查不到
        assertThrows(UsernameNotFoundException.class,
                () -> authUserDetailsService.loadUserByUsername("casetester01"));

        // 而且可以被另一个人注册，两个账号互不影响
        User lower = userService.register("casetester01", "123456");
        assertNotEquals(upper.getId(), lower.getId());
        assertEquals(upper.getId(), ((AuthUser) authUserDetailsService.loadUserByUsername("CaseTester01")).getId());
        assertEquals(lower.getId(), ((AuthUser) authUserDetailsService.loadUserByUsername("casetester01")).getId());
    }

    @Test
    void eraseCredentialsRemovesPasswordHash() {
        AuthUser user = new AuthUser(1L, "someone", "hash");

        user.eraseCredentials();

        assertNull(user.getPassword());
    }
}