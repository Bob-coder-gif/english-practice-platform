package com.jay.englishpracticeplatform.agent.tool;

import com.jay.englishpracticeplatform.entity.UserWord;
import com.jay.englishpracticeplatform.service.StudyService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** 错题本：复用错题本页面的排序（答错次数多的在前） */
@Component
public class GetWrongWordsTool implements Tool {

    private final StudyService studyService;

    public GetWrongWordsTool(StudyService studyService) {
        this.studyService = studyService;
    }

    @Override
    public String name() {
        return "get_wrong_words";
    }

    @Override
    public String description() {
        return "获取当前用户错题本中的单词：按答错次数从多到少，最多 " + StudyService.MISTAKES_PAGE_SIZE
                + " 个，附带答错次数和当前连续答对次数（连续答对 " + StudyService.MASTERED_STREAK
                + " 次后自动移出错题本）。用户问自己常错哪些词、要针对错题练习或出题时使用。";
    }

    @Override
    public ObjectNode parameters() {
        return Tool.noParams();
    }

    @Override
    public String execute(JsonNode args, ToolContext ctx) {
        Page<UserWord> page = studyService.listMistakes(ctx.userId(), 0);
        if (page.isEmpty()) {
            return "错题本为空";
        }

        StringBuilder sb = new StringBuilder("错题本共 " + page.getTotalElements() + " 个");
        if (page.getTotalElements() > page.getNumberOfElements()) {
            sb.append("，以下是答错最多的 ").append(page.getNumberOfElements()).append(" 个");
        }
        sb.append("：\n");
        for (UserWord uw : page) {
            sb.append(uw.getWord().getSpelling())
                    .append(" = ").append(SearchWordsTool.shorten(uw.getWord().getMeaning()))
                    .append("（答错 ").append(uw.getUnknownCount())
                    .append(" 次，连续答对 ").append(uw.getStreak()).append(" 次）\n");
        }
        return sb.toString();
    }
}