package com.jay.englishpracticeplatform.repository;

import com.jay.englishpracticeplatform.entity.UserWord;
import com.jay.englishpracticeplatform.entity.WordLevel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserWordRepository extends JpaRepository< UserWord, Long> {

    Optional<UserWord> findByUserIdAndWordId(Long userId, Long wordId);

    @Query("""
            select count(uw) from UserWord uw
            join uw.word w
            join w.levels l
            where uw.user.id = :userId and l = :level        
           """)
    long countLearnedByLevel(Long userId, WordLevel level);
}
