package com.jay.englishpracticeplatform.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/** AI 助手的每日提问次数（agent_usage 表）：只有计数，用 JdbcClient 直接写 SQL，不建实体 */
@Repository
public class AgentUsageRepository {

    private final JdbcClient jdbcClient;

    public AgentUsageRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    // 确保这个用户当天的那一行存在；已存在时什么也不改
    // 不用 INSERT IGNORE：它会把外键错误等其他问题也一起变成警告吞掉
    public void ensureRow(long userId, LocalDate day) {
        jdbcClient.sql("""
                        INSERT INTO agent_usage (user_id, usage_date, request_count)
                        VALUES (?, ?, 0)
                        ON DUPLICATE KEY UPDATE request_count = request_count
                        """)
                .params(userId, day)
                .update();
    }

    // 次数还没到上限时加一，返回是否加上了
    // 「判断」和「加一」在同一条 UPDATE 里完成：InnoDB 更新时会锁住这一行，
    // 两个请求同时到达也不会都读到 29、都加成 30 而超出上限
    public boolean incrementIfBelow(long userId, LocalDate day, int limit) {
        int updated = jdbcClient.sql("""
                        UPDATE agent_usage
                        SET request_count = request_count + 1
                        WHERE user_id = ? AND usage_date = ? AND request_count < ?
                        """)
                .params(userId, day, limit)
                .update();
        return updated == 1;
    }

    // 这个用户当天已经用了几次；还没有记录时是 0
    public int countForUser(long userId, LocalDate day) {
        return jdbcClient.sql("SELECT request_count FROM agent_usage WHERE user_id = ? AND usage_date = ?")
                .params(userId, day)
                .query(Integer.class)
                .optional()
                .orElse(0);
    }

    // 全站当天一共用了几次
    public long countAll(LocalDate day) {
        return jdbcClient.sql("SELECT COALESCE(SUM(request_count), 0) FROM agent_usage WHERE usage_date = ?")
                .param(day)
                .query(Long.class)
                .single();
    }
}