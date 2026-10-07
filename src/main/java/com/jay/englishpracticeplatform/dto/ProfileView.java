package com.jay.englishpracticeplatform.dto;

import java.time.LocalDate;
import java.util.Locale;

public record ProfileView (
        String username,
        LocalDate registeredOn,     //注册日期
        long daysSinceRegistered,   //注册第几天
        long learnedCount,          //累计学过的单词
        long mistakeCount,          //错题本中的单词
        long totalAnswers,          //累计答题数
        long correctAnswers,        //累计答对数
        long checkInDays,           //累计打卡天数
        int currentStreak,         //当前连续打卡
        int longestStreak,          //最长连续打卡
        int dailyGoal               //每日目标
){
    public int accuracyPercent(){
        return totalAnswers == 0 ? 0 : (int) Math.round(correctAnswers * 100.0 / totalAnswers);
    }

    //头像上显示的子，用户名的第一个字符，转为大写
    public String avatarText(){
        return username.substring(0,1).toUpperCase();
    }
}
