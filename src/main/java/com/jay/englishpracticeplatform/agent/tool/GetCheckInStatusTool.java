package com.jay.englishpracticeplatform.agent.tool;

import com.jay.englishpracticeplatform.dto.CheckInView;
import com.jay.englishpracticeplatform.service.CheckInService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.YearMonth;

/** 打卡状态：复用打卡页的 CheckInService */
@Component
public class GetCheckInStatusTool implements Tool {

    private final CheckInService checkInService;

    public GetCheckInStatusTool(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @Override
    public String name() {
        return "get_checkin_status";
    }

    @Override
    public String description() {
        return "获取当前用户的打卡情况：今日已答题数、每日目标、今天是否已打卡、还差几题、"
                + "当前连续打卡天数、累计打卡天数。用户问打卡、连续天数、今天还要做多少题时使用。";
    }

    @Override
    public ObjectNode parameters() {
        return Tool.noParams();
    }

    @Override
    public String execute(JsonNode args, ToolContext ctx) {
        // getView 会顺带生成本月日历，这里用不上，但复用现成的逻辑比再写一套查询更不容易出错
        CheckInView v = checkInService.getView(ctx.userId(), YearMonth.now());
        return "今日已答题：" + v.todayCount() + " 题，每日目标 " + v.goal() + " 题\n"
                + "今天" + (v.checkedInToday() ? "已打卡" : "还没打卡，还差 " + v.remaining() + " 题") + "\n"
                + "当前连续打卡：" + v.currentStreak() + " 天\n"
                + "累计打卡：" + v.totalDays() + " 天";
    }
}