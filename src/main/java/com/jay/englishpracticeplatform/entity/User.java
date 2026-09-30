package com.jay.englishpracticeplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}
