package com.jay.englishpracticeplatform.entity;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "answer_records",
        indexes = @Index(
                name = "idx_answer_records_user_answered_at",
                columnList = "user_id, answered_at"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnswerRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id", nullable = false)
    private Word word;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private AnswerMode mode;

    @Column(nullable = false)
    private boolean correct;

    @Column(nullable = false, updatable = false)
    private LocalDateTime answeredAt;

    public AnswerRecord(User user, Word word, AnswerMode mode, boolean correct, LocalDateTime answeredAt) {
        this.user = user;
        this.word = word;
        this.mode = mode;
        this.correct = correct;
        this.answeredAt = answeredAt;
    }
}
