package com.jay.englishpracticeplatform;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

//这里为什么不@SpringBootTest 这样写？
class PasswordEncoderTest {

    //final 和 cpp 里的const用法相似？
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();


    //encoder.encode()是做什么的？ 给密码哈希计算加密的？
    @Test
    void samePasswordProducesDifferentHashes() {
        String hash1 = encoder.encode("123456");
        String hash2 = encoder.encode("123456");

        //什么叫盐不同？
        assertNotEquals(hash1, hash2);
    }

    @Test
    void matchesWorksCorrectly() {
        String hash = encoder.encode("123456");

        assertTrue(encoder.matches("123456", hash));
        assertFalse(encoder.matches("1234567", hash));
    }
}
