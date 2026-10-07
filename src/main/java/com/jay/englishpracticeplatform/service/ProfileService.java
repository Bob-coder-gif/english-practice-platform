package com.jay.englishpracticeplatform.service;

import com.jay.englishpracticeplatform.dto.ProfileView;
import com.jay.englishpracticeplatform.entity.CheckIn;
import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.repository.AnswerRecordRepository;
import com.jay.englishpracticeplatform.repository.CheckInRepository;
import com.jay.englishpracticeplatform.repository.UserRepository;
import com.jay.englishpracticeplatform.repository.UserWordRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.attribute.UserPrincipal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final UserWordRepository userWordRepository;
    private final AnswerRecordRepository answerRecordRepository;
    private final CheckInRepository checkInRepository;

    public ProfileService(UserRepository userRepository,
                          UserWordRepository userWordRepository,
                          AnswerRecordRepository answerRecordRepository,
                          CheckInRepository checkInRepository){
        this.userRepository = userRepository;
        this.userWordRepository = userWordRepository;
        this.answerRecordRepository = answerRecordRepository;
        this.checkInRepository = checkInRepository;
    }

    @Transactional(readOnly = true)
    public ProfileView getProfile(Long userId){
        User user = userRepository.findById(userId).orElseThrow();
        LocalDate today = LocalDate.now();
        LocalDate registeredOn = user.getCreatedAt().toLocalDate();

        //累计答题，没有任何答题时，sum的结果是null
        var summary = answerRecordRepository.summarize(userId);
        long correctAnswers = summary.getCorrect() == null ? 0 : summary.getCorrect();

        //打卡，当前连续，最长连续，累计天数都基于同一份日期列表计算
        List<LocalDate> checkDates = checkInRepository.findAllDates(userId);

        return new ProfileView(
                user.getUsername(),
                registeredOn,
                ChronoUnit.DAYS.between(registeredOn, today) + 1,
                userWordRepository.countByUserId(userId),
                userWordRepository.countMistakes(userId,StudyService.MASTERED_STREAK),
                summary.getTotal(),
                correctAnswers,
                checkDates.size(),
                CheckInService.calculateStreak(checkDates,today),
                CheckInService.calculateLongestStreak(checkDates),
                user.getDailyGoal()
        );
    }
}
