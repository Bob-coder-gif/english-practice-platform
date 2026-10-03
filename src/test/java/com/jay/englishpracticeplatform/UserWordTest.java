package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.entity.UserWord;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class UserWordTest {

    private final LocalDateTime now = LocalDateTime.of(2026,10,1,8,0);

    @Test
    void reviewIntervalGrowsWithStreak(){
        UserWord userWord = new UserWord(null, null);

        int[] expectedDays = {1,2,4,7,15,30,30,30};
        for(int days : expectedDays){
            userWord.markKnown(now);
            assertEquals(now.plusDays(days), userWord.getNextReviewAt());
        }

        assertEquals(8,userWord.getKnownCount());
        assertEquals(8,userWord.getStreak());
    }

    @Test
    void unknownResetsStreakAndNeedsReviewNow(){
        UserWord userWord = new UserWord(null, null);
        userWord.markKnown(now);
        userWord.markKnown(now);

        userWord.markUnknown(now);

        assertEquals(0,userWord.getStreak());
        assertEquals(now,userWord.getNextReviewAt());
        assertEquals(2,userWord.getKnownCount());
        assertEquals(1, userWord.getUnknownCount());

        //清零后在认识一次，间隔时间从1天开始
        userWord.markKnown(now);
        assertEquals(now.plusDays(1),userWord.getNextReviewAt());
    }
}
