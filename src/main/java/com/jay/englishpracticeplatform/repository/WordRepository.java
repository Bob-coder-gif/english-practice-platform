package com.jay.englishpracticeplatform.repository;

import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.entity.WordLevel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WordRepository extends JpaRepository<Word, Long>{

    @Query("select count(w) from Word w join w.levels l where l = :level")
    long countByLevel(WordLevel level);
}
