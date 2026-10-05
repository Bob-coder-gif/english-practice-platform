package com.jay.englishpracticeplatform.repository;

import com.jay.englishpracticeplatform.entity.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface CheckInRepository extends JpaRepository<CheckIn, Long>{

    boolean existsByUserIdAndCheckDate(Long userId, LocalDate checkDate);

    long countByUserId(Long userId);

    //某个用户的所有打卡日期
    @Query("select c.checkDate from CheckIn c where c.user.id =  :userId")
    List<LocalDate> findAllDates(Long userId);

    @Query("""
            select c.checkDate from CheckIn c
            where c.user.id = :userId and c.checkDate >= :start and c.checkDate < :end
           """)
    List<LocalDate> findDatesBetween(Long userId, LocalDate start, LocalDate end);
}
