package com.jay.englishpracticeplatform.service;

import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.exception.AccountUpdateException;
import com.jay.englishpracticeplatform.repository.UserRepository;
import com.jay.englishpracticeplatform.security.JdbcClientTokenRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// 账号设置：修改用户名、修改密码
@Service
public class AccountService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("M月d日 HH:mm");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcClientTokenRepository tokenRepository;

    public AccountService(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JdbcClientTokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
    }

    // 下次可以修改用户名的时间；现在就能改时返回 null
    @Transactional(readOnly = true)
    public LocalDateTime nextUsernameChangeAt(Long userId, LocalDateTime now) {
        User user = userRepository.findById(userId).orElseThrow();
        return user.canChangeUsername(now) ? null : user.nextUsernameChangeAt();
    }

    // 修改用户名
    // 用户表（JPA）和令牌表（JdbcClient）在同一个事务里更新：
    // 两者使用同一个数据源，Spring 会让它们共用同一个数据库连接，任何一步失败都会一起回滚
    @Transactional
    public User changeUsername(Long userId, String newUsername, String currentPassword, LocalDateTime now) {
        User user = userRepository.findById(userId).orElseThrow();

        // 先验证密码：密码不对时，不透露新用户名是否已被占用等任何信息
        checkCurrentPassword(user, currentPassword);

        // 用户名区分大小写，所以 Jay → jay 不算相同，可以修改
        if (user.getUsername().equals(newUsername)) {
            throw new AccountUpdateException("newUsername", "新用户名与当前用户名相同");
        }
        if (!user.canChangeUsername(now)) {
            throw new AccountUpdateException("newUsername",
                    "用户名每 " + User.USERNAME_CHANGE_COOLDOWN_DAYS + " 天只能修改一次，下次可修改时间："
                            + user.nextUsernameChangeAt().format(TIME_FORMAT));
        }
        if (userRepository.existsByUsername(newUsername)) {
            throw new AccountUpdateException("newUsername", "该用户名已被占用");
        }

        String oldUsername = user.getUsername();
        user.changeUsername(newUsername, now);
        try {
            // 立即执行 UPDATE：如果两个人同时改成同一个名字，唯一索引会在这里报错
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new AccountUpdateException("newUsername", "该用户名已被占用");
        }

        // 改名不是安全事件：令牌里的用户名跟着换掉，其他设备上的「记住我」继续有效
        tokenRepository.updateUsername(oldUsername, newUsername);
        return user;
    }

    // 修改密码
    @Transactional
    public User changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId).orElseThrow();

        checkCurrentPassword(user, currentPassword);

        // 数据库里只有哈希值，只能用 matches 比较，不能直接比较字符串
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new AccountUpdateException("newPassword", "新密码不能与当前密码相同");
        }

        // 实体处于托管状态，修改字段后事务提交时会自动执行 UPDATE（脏检查）
        user.setPasswordHash(passwordEncoder.encode(newPassword));

        // 改密码往往是因为怀疑密码泄露，所以删除所有「记住我」令牌，让其他设备的自动登录全部失效
        tokenRepository.removeUserTokens(user.getUsername());
        return user;
    }

    private void checkCurrentPassword(User user, String currentPassword) {
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new AccountUpdateException("currentPassword", "当前密码不正确");
        }
    }
}