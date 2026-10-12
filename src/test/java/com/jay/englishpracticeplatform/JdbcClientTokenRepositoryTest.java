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

    @Test
    void removeUserTokensIsCaseSensitive() {
        // 防止的就是这个坑：Token_User 和 token_user 是两个用户，
        // 如果 username 列不区分大小写，删除一个人的令牌会把另一个人的也删掉
        repository.createNewToken(newToken("Token_User", "series-upper", "t1"));
        repository.createNewToken(newToken("token_user", "series-lower", "t2"));

        repository.removeUserTokens("token_user");

        assertNotNull(repository.getTokenForSeries("series-upper"));
        assertNull(repository.getTokenForSeries("series-lower"));
    }

    @Test
    void updateUsernameRenamesOnlyThatUsersTokens() {
        repository.createNewToken(newToken("rename_old", "series-r1", "t1"));
        repository.createNewToken(newToken("rename_old", "series-r2", "t2"));
        repository.createNewToken(newToken("someone_else", "series-r3", "t3"));

        int updated = repository.updateUsername("rename_old", "rename_new");

        assertEquals(2, updated);
        assertEquals("rename_new", repository.getTokenForSeries("series-r1").getUsername());
        assertEquals("rename_new", repository.getTokenForSeries("series-r2").getUsername());
        // 令牌本身没变，只是换了用户名
        assertEquals("t1", repository.getTokenForSeries("series-r1").getTokenValue());
        assertEquals("someone_else", repository.getTokenForSeries("series-r3").getUsername());
    }
}