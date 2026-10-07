package com.jay.englishpracticeplatform.repository;

import com.jay.englishpracticeplatform.entity.AnswerMode;
import com.jay.englishpracticeplatform.entity.AnswerRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AnswerRecordRepository extends JpaRepository<AnswerRecord, Long> {

    // 按插入顺序查出某个用户的所有答题记录（测试中使用）
    List<AnswerRecord> findByUserIdOrderByIdAsc(Long userId);

    //某段时间内 按答题模式分组统计
    @Query("""
            select ar.mode as mode,
                 count(ar) as total,
                 sum (case when ar.correct = true then 1 else 0 end) as correct
            from AnswerRecord  ar
            where ar.user.id = :userId
                 and ar.answeredAt >= :start and ar.answeredAt < :end
            group by ar.mode
            """)
    List<ModeCountView> countByMode(Long userId, LocalDateTime start, LocalDateTime end);

    //某段时间内，按日期分组统计
    @Query("""
             select cast(ar.answeredAt as localdate ) as answerDate,
                        count(ar) as total
             from AnswerRecord ar
             where ar.user.id = :userId
                 and ar.answeredAt >= :start and ar.answeredAt < :end
             group by cast(ar.answeredAt as localdate ) 
            """)
    List<DailyCountView> countByDay(Long userId, LocalDateTime start, LocalDateTime end);

    // 累计的答题数和答对数
    @Query("""
            select count(ar) as total,
                   sum(case when ar.correct = true then 1 else 0 end) as correct
            from AnswerRecord ar
            where ar.user.id = :userId
            """)
    SummaryView summarize(Long userId);

    //某段时间内的答题总数
    @Query("""
            select count(ar) from AnswerRecord ar
            where ar.user.id = :userId
                and ar.answeredAt >= :start and ar.answeredAt < :end
            """)
    long countBetween(Long userId, LocalDateTime start, LocalDateTime end);

    // ========================统计查询的结果类型 ================
    interface ModeCountView {
        AnswerMode getMode();

        Long getTotal();

        Long getCorrect();
    }

    interface DailyCountView {
        LocalDate getAnswerDate();

        Long getTotal();
    }

    interface SummaryView {
        Long getTotal();

        Long getCorrect();
    }
}
