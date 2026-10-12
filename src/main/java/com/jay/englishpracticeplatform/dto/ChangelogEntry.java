package com.jay.englishpracticeplatform.dto;

import java.time.LocalDate;
import java.util.List;

// 一条更新日志，对应 changelog.json 里的一个对象
// id 越大越新，id 最大的那条就是「最近一次更新」
public record ChangelogEntry(
        int id,
        LocalDate date,
        String title,
        List<String> items
) {
}