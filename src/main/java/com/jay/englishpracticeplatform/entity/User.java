package com.jay.englishpracticeplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity                     // 这个类对应数据库里的一张表
@Table(name = "users")      // 表名叫 users
@Getter                     // lombok 自动生成所有getXXX（）方法
@Setter                     // lombok 自动生成所有setXXX（）方法
@NoArgsConstructor          // Lombok 自动生成无参数构造方法
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static final int DEFAULT_DAILY_GOAL = 20;

    @ColumnDefault("20")
    @Column(nullable = false)
    private int dailyGoal = DEFAULT_DAILY_GOAL;

    // 两次修改用户名之间至少间隔的天数
    public static final int USERNAME_CHANGE_COOLDOWN_DAYS = 3;

    // 上次修改用户名的时间；从没改过时为 null
    private LocalDateTime usernameChangedAt;

    public LocalDateTime nextUsernameChangeAt(){
        if(usernameChangedAt == null){
            return null;
        }
        return usernameChangedAt.plusDays(USERNAME_CHANGE_COOLDOWN_DAYS);
    }

    public boolean canChangeUsername(LocalDateTime now){
        LocalDateTime next = nextUsernameChangeAt();
        return next == null || !now.isBefore(next);
    }
    // 修改用户名，同时记录修改时间，保证两者总是一起变化
    // Service 会先调用 canChangeUsername 给出友好提示，这里的检查是最后一道防线
    public void changeUsername(String  newName,LocalDateTime now){
        if(!canChangeUsername(now)){
            throw new IllegalArgumentException("用户名修改过于频繁");
        }
        this.username = newName;
        this.usernameChangedAt = now;
    }
}
