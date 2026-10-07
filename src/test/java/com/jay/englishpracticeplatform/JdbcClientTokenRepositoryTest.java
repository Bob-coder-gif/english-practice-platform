package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.security.JdbcClientTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class JdbcClientTokenRepositoryTest {

    @Autowired
    private JdbcClientTokenRepository repository;

    private PersistentRememberMeToken newToken(String username, String series, String token) {
        return new PersistentRememberMeToken(username, series, token, new Date());
    }

    @Test
    void createAndFindToken() {
        repository.createNewToken(newToken("token_user", "series-1", "token-1"));

        PersistentRememberMeToken found = repository.getTokenForSeries("series-1");

        assertNotNull(found);
        assertEquals("token_user", found.getUsername());
        assertEquals("token-1", found.getTokenValue());
    }

    @Test
    void unknownSeriesReturnsNull() {
        assertNull(repository.getTokenForSeries("no-such-series"));
    }

    @Test
    void updateReplacesTokenValue() {
        repository.createNewToken(newToken("token_user", "series-2", "old-token"));

        repository.updateToken("series-2", "new-token", new Date());

        assertEquals("new-token", repository.getTokenForSeries("series-2").getTokenValue());
    }

    @Test
    void removeUserTokensDeletesAllSeriesOfThatUser() {
        repository.createNewToken(newToken("token_user", "series-a", "t1"));
        repository.createNewToken(newToken("token_user", "series-b", "t2"));
        repository.createNewToken(newToken("other_user", "series-c", "t3"));

        repository.removeUserTokens("token_user");

        assertNull(repository.getTokenForSeries("series-a"));
        assertNull(repository.getTokenForSeries("series-b"));
        // 其他用户的令牌不受影响
        assertNotNull(repository.getTokenForSeries("series-c"));
    }
}