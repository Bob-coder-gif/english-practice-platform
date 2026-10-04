package com.jay.englishpracticeplatform.repository;

import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.entity.UserWord;
import com.jay.englishpracticeplatform.entity.WordLevel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface UserWordRepository extends JpaRepository< UserWord, Long> {

    Optional<UserWord> findByUserIdAndWordId(Long userId, Long wordId);

    @Query("""
            select count(uw) from UserWord uw
            join uw.word w
            join w.levels l
            where uw.user.id = :userId and l = :level        
           """)
    long countLearnedByLevel(Long userId, WordLevel level);

    //复习， 已到期的单词，按到期时间从早到晚
    @Query("""
            select uw from UserWord  uw
            join fetch uw.word
            where uw.user.id = :userId and uw.nextReviewAt <= :now
            order by uw.nextReviewAt
            """)
    List<UserWord> findDueWords(Long userId, LocalDateTime now, Pageable pageable);

    //复习， 已到期的单词
    long countByUserIdAndNextReviewAtLessThanEqual(Long userId, LocalDateTime now);

    //错题本：不认识过，并且没有连续认识足够次数的单词
    @Query(
            value = """
                        select uw from UserWord uw
                        join fetch  uw.word
                        where uw.user.id = :userId
                            and uw.unknownCount > 0 
                            and uw.streak < :masteredStreak
                    """,
            countQuery = """
                        select count(uw) from UserWord  uw
                        where uw.user.id = :userId
                            and uw.unknownCount > 0
                            and uw.streak < :masteredStreak
                    """
    )
    Page<UserWord> findMistakes(Long userId, int masteredStreak, Pageable pageable);

    Long user(User user);

    //累计学过多少个单词
    long countByUserId(Long userId);

    //错题泵中国的单词数量
    @Query("""
           select count(uw) from UserWord uw
           where uw.user.id = :userId
                and uw.unknownCount > 0 
                and uw.streak < :masteredStreak
           
           """)
    long countMistakes(Long userId, int masteredStreak);
}
