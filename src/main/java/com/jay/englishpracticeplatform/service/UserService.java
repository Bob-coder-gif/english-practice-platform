package com.jay.englishpracticeplatform.service;

import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.exception.InvalidCredentialsException;
import com.jay.englishpracticeplatform.exception.UsernameAlreadyExistsException;
import com.jay.englishpracticeplatform.repository.UserRepository;
import com.jay.englishpracticeplatform.exception.InvalidCredentialsException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    //构造器注入：Spring 创建 UserService 时，自动把这两个依赖传进来
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String username, String rawPassword){
        // 检查用户名是否已被占用
        if(userRepository.existsByUsername(username)){
            throw new UsernameAlreadyExistsException(username);
        }

        // 对密码做哈希，数据库只存储哈希值
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));

        try {
            return userRepository.saveAndFlush(user);
        }
        catch (DataIntegrityViolationException e){
            // 兜底，两个请求同时注册一个用户名时
            throw new UsernameAlreadyExistsException(username);
        }
    }

    @Transactional(readOnly = true)
    public User login(String username, String rawPassword){
        User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new InvalidCredentialsException());

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())){
            throw new InvalidCredentialsException();
        }

        return user;
    }

}
