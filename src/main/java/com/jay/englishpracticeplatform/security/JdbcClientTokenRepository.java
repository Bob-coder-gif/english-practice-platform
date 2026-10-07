package com.jay.englishpracticeplatform.security;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Date;


// [记住我] 令牌的存储， 用 JdncClient 读写 persistent_login 表
// 替代 Spring Security 自带的 JdbcTokenRepositoryImpl（他继承的  JdbcDaoSupport 在 Spring 7 中被标记为将被删除）
@Repository
public class JdbcClientTokenRepository implements PersistentTokenRepository {

    private final JdbcClient jdbcClient;

    public JdbcClientTokenRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    //勾选 [记住我] 登录是，保存一条新令牌
    @Override
    public void createNewToken(PersistentRememberMeToken token) {
        jdbcClient.sql("""
                        INSERT INTO persistent_logins (username, series, token, last_used)
                        VALUES (:username, :series, :token, :lastUsed)
                        """)
                .param("username", token.getUsername())
                .param("series", token.getSeries())
                .param("token", token.getTokenValue())
                .param("lastUsed", new Timestamp(token.getDate().getTime()))
                .update();
    }

    //每次用令牌自动登录后，更换令牌
    @Override
    public void updateToken(String series, String tokenValue, Date lastUsed) {
        jdbcClient.sql("""
                        UPDATE persistent_logins 
                        SET token = :token, last_used = :lastUsed
                        WHERE series = :series
                        """)
                .param("token", tokenValue)
                .param("lastUsed", new Timestamp(lastUsed.getTime()))
                .param("series", series)
                .update();
    }

    @Override
    //自动登录是，根据序列号查找令牌，找不到时按接口约定返回null
    public PersistentRememberMeToken getTokenForSeries(String series) {
        return jdbcClient.sql("""
                        SELECT username, series, token, last_used
                        FROM persistent_logins
                        WHERE series = :series
                        """)
                .param("series", series)
                .query((rs, rowNum) -> new PersistentRememberMeToken(
                        rs.getString("username"),
                        rs.getString("series"),
                        rs.getString("token"),
                        rs.getTimestamp("last_used")
                ))
                .optional()
                .orElse(null);
    }

    //退出登录，发现令牌被盗时，删除该用户所有令牌（后续添加修改密码功能）
    @Override
    public void removeUserTokens(String username) {
        jdbcClient.sql("DELETE FROM persistent_logins WHERE username = :username")
                .param("username", username)
                .update();
    }

}
