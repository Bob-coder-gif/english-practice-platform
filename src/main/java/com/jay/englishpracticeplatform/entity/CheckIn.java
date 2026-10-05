package com.jay.englishpracticeplatform.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "check_ins",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_check_ins_user_date",
                columnNames = {"user_id", "check_date"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate checkDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createAt;

    public CheckIn(User user, LocalDate checkDate, LocalDateTime createAt){
        this.user = user;
        this.checkDate = checkDate;
        this.createAt = createAt;
    }
}
