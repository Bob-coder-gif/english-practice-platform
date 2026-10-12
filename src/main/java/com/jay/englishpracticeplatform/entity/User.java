package com.jay.englishpracticeplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity                     // 这个类对应数据库里的一张表
@Table(name = "users")      // 表名叫 users
@Getter                     // lombok 自动生成所有getXXX（）方法
@Setter                     // lombok 自动生成所有setXXX（）方法
@NoArgsConstructor          // Lombok 自动生成无参数构造方法
public class User {

    public static final int DEFAULT_DAILY_GOAL = 20;

    // 两次修改用户名之间至少间隔的天数
    public static final int USERNAME_CHANGE_COOLDOWN_DAYS = 3;

    // 同一条更新，登录后最多弹出几次
    public static final int CHANGELOG_POPUP_LIMIT = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ColumnDefault("20")
    @Column(nullable = false)
    private int dailyGoal = DEFAULT_DAILY_GOAL;

    // 上次修改用户名的时间；从没改过时为 null
    // 没有写 @Column：JPA 默认映射到 username_changed_at 列，并且允许为空
    private LocalDateTime usernameChangedAt;

    // 已弹出过的最新一条更新的编号；从没弹出过时为 null
    private Integer seenChangelogId;

    // 这条更新已经弹出的次数
    @ColumnDefault("0")
    @Column(nullable = false)
    private int changelogShownCount = 0;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ==================== 修改用户名 ====================

    // 下次可以修改用户名的时间；从没改过时返回 null，表示随时可以改
    public LocalDateTime nextUsernameChangeAt() {
        if (usernameChangedAt == null) {
            return null;
        }
        return usernameChangedAt.plusDays(USERNAME_CHANGE_COOLDOWN_DAYS);
    }

    // 现在能否修改用户名：从没改过，或者已经过了冷却期
    public boolean canChangeUsername(LocalDateTime now) {
        LocalDateTime next = nextUsernameChangeAt();
        return next == null || !now.isBefore(next);
    }

    // 修改用户名，同时记录修改时间，保证两者总是一起变化
    // Service 会先调用 canChangeUsername 给出友好提示，这里的检查是最后一道防线
    public void changeUsername(String newUsername, LocalDateTime now) {
        if (!canChangeUsername(now)) {
            throw new IllegalStateException("用户名修改过于频繁");
        }
        this.username = newUsername;
        this.usernameChangedAt = now;
    }

    // ==================== 更新日志弹窗 ====================

    // 登录时调用：这条更新还没弹够次数，就计一次并返回 true；否则返回 false
    // 出了新的更新（latestId 变了），次数从 0 重新开始
    public boolean recordChangelogPopup(int latestId) {
        if (seenChangelogId == null || !seenChangelogId.equals(latestId)) {
            seenChangelogId = latestId;
            changelogShownCount = 0;
        }
        if (changelogShownCount >= CHANGELOG_POPUP_LIMIT) {
            return false;
        }
        changelogShownCount++;
        return true;
    }
}