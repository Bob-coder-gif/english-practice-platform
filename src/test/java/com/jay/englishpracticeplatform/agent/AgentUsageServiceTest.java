package com.jay.englishpracticeplatform.agent;

import com.jay.englishpracticeplatform.exception.AgentQuotaExceededException;
import com.jay.englishpracticeplatform.repository.AgentUsageRepository;
import com.jay.englishpracticeplatform.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 每日限额：连本地数据库，测试结束后自动回滚。
 * 用一个很早的固定日期，不会和你本地真实使用 AI 助手产生的记录混在一起
 */
@SpringBootTest
@Transactional
class AgentUsageServiceTest {

    private static final LocalDate DAY = LocalDate.of(2000, 1, 1);

    @Autowired
    private AgentUsageRepository repository;

    @Autowired
    private UserService userService;

    // 直接 new，用测试自己的上限，不受 application.properties 里的配置影响
    private AgentUsageService service(int dailyLimit, int globalDailyLimit) {
        AgentProperties props = new AgentProperties(true, "test-key", "http://localhost", "test-model",
                100, 5, Duration.ofSeconds(5), dailyLimit, globalDailyLimit);
        return new AgentUsageService(repository, props);
    }

    private long newUser(String username) {
        return userService.register(username, "123456").getId();
    }

    @Test
    void newUserHasFullQuota() {
        long userId = newUser("usage_new");

        assertEquals(new AgentUsageService.Quota(0, 2, 2), service(2, 0).quota(userId, DAY));
    }

    @Test
    void userLimitResetsNextDay() {
        long userId = newUser("usage_daily");
        AgentUsageService service = service(2, 0);

        service.acquire(userId, DAY);
        service.acquire(userId, DAY);
        AgentQuotaExceededException e = assertThrows(AgentQuotaExceededException.class,
                () -> service.acquire(userId, DAY));

        assertTrue(e.getMessage().contains("2 次"));
        assertEquals(new AgentUsageService.Quota(2, 2, 0), service.quota(userId, DAY));

        // 第二天重新计数
        service.acquire(userId, DAY.plusDays(1));
        assertEquals(1, service.quota(userId, DAY.plusDays(1)).used());
    }

    @Test
    void globalLimitAppliesAcrossUsers() {
        long first = newUser("usage_global_a");
        long second = newUser("usage_global_b");
        AgentUsageService service = service(10, 3);

        service.acquire(first, DAY);
        service.acquire(first, DAY);
        service.acquire(second, DAY);

        // 两个人加起来已经 3 次，即使 second 个人只用了 1 次，也不能再问
        AgentQuotaExceededException e = assertThrows(AgentQuotaExceededException.class,
                () -> service.acquire(second, DAY));
        assertTrue(e.getMessage().contains("总额度"));
        assertEquals(1, service.quota(second, DAY).used());
    }
}