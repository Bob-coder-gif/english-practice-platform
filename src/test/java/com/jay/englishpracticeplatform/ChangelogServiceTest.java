package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.dto.ChangelogEntry;
import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.service.ChangelogService;
import com.jay.englishpracticeplatform.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ChangelogServiceTest {

    @Autowired
    private ChangelogService changelogService;

    @Autowired
    private UserService userService;

    @Test
    void entriesAreLoadedNewestFirst() {
        List<ChangelogEntry> all = changelogService.all();

        assertFalse(all.isEmpty());
        assertEquals(all.get(0), changelogService.latest());
        for (int i = 0; i + 1 < all.size(); i++) {
            assertTrue(all.get(i).id() > all.get(i + 1).id());
        }
    }

    @Test
    void popupOnLoginStopsAfterLimit() {
        User user = userService.register("changelog_tester", "123456");

        for (int i = 0; i < User.CHANGELOG_POPUP_LIMIT; i++) {
            assertTrue(changelogService.popupOnLogin(user.getId()).isPresent());
        }
        assertTrue(changelogService.popupOnLogin(user.getId()).isEmpty());
    }
}