package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindByUsername(){
        // 准备数据
        User user = new User();
        user.setUsername("test_user");
        user.setPasswordHash("fake_hash");

        // 保存
        User saved = userRepository.save(user);

        // 保存后。数据库自动生成了id， @PrePersist 自动填充时间
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        // 按用户名查询
        Optional<User> found = userRepository.findByUsername("test_user");
        assertTrue(found.isPresent());
        assertEquals("fake_hash", found.get().getPasswordHash());
    }

    @Test
    void existsByUsername(){
        User user = new User();
        user.setUsername("exists_user");
        user.setPasswordHash("fake_hash");
        userRepository.save(user);

        assertTrue(userRepository.existsByUsername("exists_user"));
        assertFalse(userRepository.existsByUsername("nobody"));
    }
}
