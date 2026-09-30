package com.jay.englishpracticeplatform.repository;

import com.jay.englishpracticeplatform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    //根据用户名查找用户（登录时使用)
    Optional<User> findByUsername(String username);

    // 判断用户名是否已存在（注册时使用)
    boolean existsByUsername(String username);
}
