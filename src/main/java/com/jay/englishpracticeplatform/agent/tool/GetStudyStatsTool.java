package com.jay.englishpracticeplatform.agent.tool;

import com.jay.englishpracticeplatform.dto.StatsView;
import com.jay.englishpracticeplatform.service.StatsService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** 学习统计：直接复用统计页的 StatsService，数据和页面上看到的完全一致 */
@Component
public class GetStudyStatsTool implements Tool {

    private final StatsService statsService;

    public GetStudyStatsTool(StatsService statsService) {
        this.statsService = statsService;
    }

    @Override
    public String name() {
        return "get_study_stats";
    }

    @Override
    public String description() {
        return "获取当前用户的学习统计：今日各模式答题数、累计学过的单词数、待复习数、错题数、累计正确率、"
                + "各考试级别（高考/四级/六级/考研/雅思）的学习进度、最近 7 天每天的答题数。"
                + "用户询问学习情况、进度、正确率，或需要据此给出学习建议时使用。";
    }

    @Override
    public ObjectNode parameters() {
        return Tool.noParams();
    }

    @Override
    public String execute(JsonNode args, ToolContext ctx) {
        StatsView s = statsService.getStats(ctx.userId(), 7);

        StringBuilder sb = new StringBuilder();
        sb.append("今日答题：").append(s.todayTotal()).append(" 题（");
        for (int i = 0; i < s.todayByMode().size(); i++) {
            StatsView.ModeStat m = s.todayByMode().get(i);
            if (i > 0) sb.append("，");
            sb.append(m.mode().getLabel()).append(' ').append(m.total())
                    .append(" 题对 ").append(m.correct());
        }
        sb.append("）\n");

        sb.append("累计学过单词：").append(s.learnedCount()).append(" 个\n");
        sb.append("当前待复习：").append(s.dueCount()).append(" 个\n");
        sb.append("错题本：").append(s.mistakeCount()).append(" 个\n");
        sb.append("累计答题：").append(s.totalAnswers()).append(" 题，答对 ")
                .append(s.correctAnswers()).append(" 题，正确率 ")
                .append(s.accuracyPercent()).append("%\n");

        sb.append("各级别进度：");
        for (int i = 0; i < s.levelProgress().size(); i++) {
            StatsView.LevelProgress p = s.levelProgress().get(i);
            if (i > 0) sb.append("，");
            sb.append(p.level().getLabel()).append(' ').append(p.learned()).append('/')
                    .append(p.total()).append("（").append(p.percent()).append("%）");
        }
        sb.append('\n');

        sb.append("最近 7 天每天答题数：");
        for (int i = 0; i < s.daily().size(); i++) {
            StatsView.DailyStat d = s.daily().get(i);
            if (i > 0) sb.append("，");
            sb.append(d.day()).append(' ').append(d.total());
        }
        return sb.toString();
    }
}