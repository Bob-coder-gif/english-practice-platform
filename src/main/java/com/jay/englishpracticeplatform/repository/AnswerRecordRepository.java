package com.jay.englishpracticeplatform.repository;

import com.jay.englishpracticeplatform.entity.AnswerMode;
import com.jay.englishpracticeplatform.entity.AnswerRecord;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerRecordRepository extends JpaRepository<AnswerRecord, Long>{

    // 按插入顺序查出某个用户的所有答题记录
    List<AnswerRecord> findByUserIdOrderByIdAsc(Long userId);

    AnswerMode mode(AnswerMode mode);
}
