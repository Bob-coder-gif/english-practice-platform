package com.jay.englishpracticeplatform.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 我的词库中的一条收藏：哪个用户，收藏了哪个单词，什么时候收藏
// 只记录关系，单词本身仍在words表中，所以学习复习都能直接用
@Entity
@Table(
        name = "user_collections",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_collections_user_word",
                columnNames = {"user_id","word_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  //JPA 需要无参构造方法，但不希望业务代码直接调用
public class UserCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "word_id",nullable = false)
    private Word word;

    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;

    public UserCollection(User user,Word word, LocalDateTime createdAt){
        this.user = user;
        this.word = word;
        this.createdAt = createdAt;
    }
}
