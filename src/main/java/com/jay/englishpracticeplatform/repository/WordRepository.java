package com.jay.englishpracticeplatform.repository;

import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.entity.WordLevel;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WordRepository extends JpaRepository<Word, Long>{

    @Query("select count(w) from Word w join w.levels l where l = :level")
    long countByLevel(WordLevel level);

    @Query(
            value = "select w from Word w join w.levels l where l = :level",
            countQuery = "select count(w) from Word w join w.levels l where l = :level"
    )
    Page<Word> findByLevel(WordLevel level, Pageable pageable);

    @Query("""
            select w from Word w
            join w.levels l
            where l = :level
              and not exists (
                  select 1 from UserWord uw
                  where uw.user.id = :userId and uw.word = w
              )
            order by w.spelling
            """)
    List<Word> findNewWords(WordLevel level, Long userId, Pageable pageable);

    //某个级别中随机选出若干个单词的id
    @Query(value = """
            SELECT w.id FROM words w
            JOIN word_levels l on l.word_id = w.id
            WHERE l.level = :level
            ORDER BY RAND()
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findRandomIdsByLevel(String level, int limit);

    //某个级别中，该用户学过的单词中随机选出若干个
    @Query(value = """
            SELECT w.id FROM words w
            JOIN word_levels l ON l.word_id = w.id
            JOIN user_words uw ON uw.word_id = w.id
            WHERE l.level = :level AND uw.user_id = :userId
            ORDER BY RAND()
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findRandomLearnedIds(Long userId, String level, int limit);

}
