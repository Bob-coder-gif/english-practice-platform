package com.jay.englishpracticeplatform.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_words",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_words_user_word",
                columnNames = {"user_id", "word_id"}
        ),
        indexes = @Index(
                name = "idx_user_words_user_next_review",
                columnList = "user_id, next_review_at"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserWord {

    // 连续认识 1，2，3，4，5，6，次后，分别间隔多少天再复习
    private static final int[] REVIEW_INTERVAL_DAYS = {1, 2, 4, 7, 15, 30};

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id", nullable = false)
    private Word word;

    @Column(nullable = false)
    private int knownCount;

    @Column(nullable = false)
    private int unknownCount;

    @Column(nullable = false)
    private int streak;

    @Column(nullable = false)
    private LocalDateTime lastReviewedAt;

    @Column(nullable = false)
    private LocalDateTime nextReviewAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public UserWord(User user, Word word) {
        this.user = user;
        this.word = word;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    //用户点了 [认识]
    public void markKnown(LocalDateTime now) {
        knownCount++;
        streak++;
        int index = Math.min(streak - 1, REVIEW_INTERVAL_DAYS.length - 1);
        lastReviewedAt = now;
        nextReviewAt = now.plusDays(REVIEW_INTERVAL_DAYS[index]);
    }

    //用户点了 [不认识]
    public void markUnknown(LocalDateTime now) {
        unknownCount++;
        streak = 0;
        lastReviewedAt = now;
        nextReviewAt = now;
    }
}
