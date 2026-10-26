package com.jay.englishpracticeplatform.agent;

import com.jay.englishpracticeplatform.exception.AgentQuotaExceededException;
import com.jay.englishpracticeplatform.repository.AgentUsageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * AI 助手的每日限额。
 * 每次提问前先扣一次额度，扣不到就拒绝，不请求模型。
 * 失败的提问（比如 DeepSeek 暂时不可用）也算一次：失败前可能已经请求过模型、产生了费用。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "agent", name = "enabled", havingValue = "true")
public class AgentUsageService {

    /** 当天额度：已用、上限、剩余（record 序列化成 JSON 时只输出这三个字段） */
    public record Quota(int used, int limit, int remaining) {
        static Quota of(int used, int limit) {
            return new Quota(used, limit, Math.max(0, limit - used));
        }
    }

    private final AgentUsageRepository repository;
    private final int dailyLimit;
    private final int globalDailyLimit;

    public AgentUsageService(AgentUsageRepository repository, AgentProperties props) {
        this.repository = repository;
        this.dailyLimit = props.dailyLimit();
        this.globalDailyLimit = props.globalDailyLimit();
    }

    /** 扣一次今天的额度；用完了抛出 AgentQuotaExceededException */
    @Transactional
    public void acquire(long userId) {
        acquire(userId, LocalDate.now());
    }

    // 日期作为参数传入，测试时可以模拟「第二天」
    void acquire(long userId, LocalDate day) {
        // 全站上限：防止有人注册大量账号，每个账号都用满个人额度
        // 先查总数再扣个人额度，两步之间不加锁，高并发时可能超出几次，这里可以接受
        if (globalDailyLimit > 0 && repository.countAll(day) >= globalDailyLimit) {
            log.warn("AI 助手全站今日额度已用完：{} 次", globalDailyLimit);
            throw new AgentQuotaExceededException("今天 AI 助手的总额度已经用完了，明天再来吧");
        }

        repository.ensureRow(userId, day);
        if (!repository.incrementIfBelow(userId, day, dailyLimit)) {
            throw new AgentQuotaExceededException("今天的 " + dailyLimit + " 次提问已经用完了，明天再来吧");
        }
    }

    @Transactional(readOnly = true)
    public Quota quota(long userId) {
        return quota(userId, LocalDate.now());
    }

    Quota quota(long userId, LocalDate day) {
        return Quota.of(repository.countForUser(userId, day), dailyLimit);
    }
}