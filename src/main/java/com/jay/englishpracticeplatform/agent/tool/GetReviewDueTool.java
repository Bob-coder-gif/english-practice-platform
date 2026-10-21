package com.jay.englishpracticeplatform.agent.tool;

import com.jay.englishpracticeplatform.entity.UserWord;
import com.jay.englishpracticeplatform.service.StudyService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

/** 待复习：间隔复习中已经到期的单词 */
@Component
public class GetReviewDueTool implements Tool {

    static final int LIMIT = 20;

    private final StudyService studyService;

    public GetReviewDueTool(StudyService studyService) {
        this.studyService = studyService;
    }

    @Override
    public String name() {
        return "get_review_due";
    }

    @Override
    public String description() {
        return "获取当前用户今天到期、需要复习的单词（按到期时间从早到晚，最多 " + LIMIT + " 个），"
                + "以及到期单词的总数。用户问今天要复习什么、复习任务有多少时使用。";
    }

    @Override
    public ObjectNode parameters() {
        return Tool.noParams();
    }

    @Override
    public String execute(JsonNode args, ToolContext ctx) {
        long total = studyService.countDue(ctx.userId());
        if (total == 0) {
            return "当前没有到期需要复习的单词";
        }

        List<UserWord> due = studyService.listDue(ctx.userId(), LIMIT);
        StringBuilder sb = new StringBuilder("到期待复习共 " + total + " 个");
        if (total > due.size()) {
            sb.append("，以下是最早到期的 ").append(due.size()).append(" 个");
        }
        sb.append("：\n");
        for (UserWord uw : due) {
            sb.append(uw.getWord().getSpelling())
                    .append(" = ").append(SearchWordsTool.shorten(uw.getWord().getMeaning()))
                    .append("（连续答对 ").append(uw.getStreak()).append(" 次）\n");
        }
        return sb.toString();
    }
}