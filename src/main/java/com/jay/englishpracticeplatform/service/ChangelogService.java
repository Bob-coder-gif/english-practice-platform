package com.jay.englishpracticeplatform.service;

import com.jay.englishpracticeplatform.dto.ChangelogEntry;
import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.repository.UserRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// 更新日志：内容写在 resources/changelog.json 里，和代码一起提交、一起部署
@Service
public class ChangelogService {

    private static final String LOCATION = "changelog.json";

    private final UserRepository userRepository;

    // 按 id 从大到小排好序，第一条就是最近一次更新
    private final List<ChangelogEntry> entries;

    public ChangelogService(ObjectMapper objectMapper, UserRepository userRepository) {
        this.userRepository = userRepository;
        this.entries = load(objectMapper);
    }

    // 启动时读取并检查一次；格式不对就直接启动失败，而不是等用户访问时才出错
    private static List<ChangelogEntry> load(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource(LOCATION).getInputStream()) {
            List<ChangelogEntry> list = objectMapper.readValue(in, new TypeReference<List<ChangelogEntry>>() {
            });
            validate(list);
            return list.stream()
                    .sorted(Comparator.comparingInt(ChangelogEntry::id).reversed())
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("读取更新日志失败：" + LOCATION, e);
        }
    }

    private static void validate(List<ChangelogEntry> list) {
        if (list == null || list.isEmpty()) {
            throw new IllegalStateException("更新日志不能为空");
        }
        Set<Integer> ids = new HashSet<>();
        for (ChangelogEntry entry : list) {
            if (entry.id() <= 0 || !ids.add(entry.id())) {
                throw new IllegalStateException("更新日志的 id 必须为正数且不能重复：" + entry.id());
            }
            if (entry.date() == null || entry.title() == null || entry.title().isBlank()
                    || entry.items() == null || entry.items().isEmpty()) {
                throw new IllegalStateException("更新日志内容不完整，id = " + entry.id());
            }
        }
    }

    // 全部更新，最新的在前
    public List<ChangelogEntry> all() {
        return entries;
    }

    // 最近一次更新
    public ChangelogEntry latest() {
        return entries.get(0);
    }

    public Optional<ChangelogEntry> findById(int id) {
        return entries.stream().filter(e -> e.id() == id).findFirst();
    }

    // 用户登录时调用：最近一次更新还没弹够次数，就记一次并返回它；否则返回空
    @Transactional
    public Optional<ChangelogEntry> popupOnLogin(Long userId) {
        ChangelogEntry latest = latest();
        User user = userRepository.findById(userId).orElseThrow();
        return user.recordChangelogPopup(latest.id()) ? Optional.of(latest) : Optional.empty();
    }
}