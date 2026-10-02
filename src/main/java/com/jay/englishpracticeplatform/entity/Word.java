package com.jay.englishpracticeplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "words")
@Getter
@Setter
@NoArgsConstructor
public class Word {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String spelling;

    //音标可以为空，少数单词在 ECDICT中没有音标
    @Column(length = 64)
    private String phonetic;

    @Column(nullable = false, length = 500)
    private String meaning;

    @ElementCollection
    @CollectionTable(
            name = "word_levels",
            joinColumns = @JoinColumn(name = "word_id"),
            indexes = @Index(name = "idx_word_levels_level", columnList = "level, word_id")
    )
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "level", nullable = false , length = 20)
    private Set<WordLevel> levels = new HashSet<>();
}
