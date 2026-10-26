package com.jay.englishpracticeplatform.agent.tool;

import com.jay.englishpracticeplatform.dto.DictEntry;
import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.service.DictionaryService;
import com.jay.englishpracticeplatform.service.WordService;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * 查词：先查 words 表（考试词库 + 已被收藏进来的扩展词），不够再查扩展词典。
 * 和「我的词库」页面的搜索是同一套规则：英文按前缀，中文搜释义。
 */
@Component
public class SearchWordsTool implements Tool {

    // 返回给模型的结果数：太多会浪费 token，模型也用不上
    static final int LIMIT = 10;
    // 单条释义的最大长度：ECDICT 有些词的释义很长
    static final int MAX_MEANING_LENGTH = 80;

    private final WordService wordService;
    private final DictionaryService dictionaryService;

    public SearchWordsTool(WordService wordService, DictionaryService dictionaryService) {
        this.wordService = wordService;
        this.dictionaryService = dictionaryService;
    }

    @Override
    public String name() {
        return "search_words";
    }

    @Override
    public String description() {
        return "在平台词典中查询单词或短语。英文按开头匹配，中文在释义中查找。"
                + "解释单词含义、判断某个词是否存在时，必须先用它查询，并以查询结果为准，不要自己编造释义。"
                + "如果用户给的是过去式、复数等变形，用原形查询。";
    }

    @Override
    public ObjectNode parameters() {
        return Tool.oneStringParam("query", "要查询的英文单词（或开头部分），或中文释义中的词");
    }

    @Override
    public String execute(JsonNode args, ToolContext ctx) {
        // 长度限制、去空格由 WordService.normalizeKeyword 统一处理，这里不重复写
        String query = args.path("query").asString("");
        if (WordService.normalizeKeyword(query).isEmpty()) {
            return "错误：query 不能为空";
        }

        StringBuilder sb = new StringBuilder();
        int count = 0;

        // 1. words 表：listWords 已经把「以搜索词开头的」排在前面
        List<Word> words = wordService.listWords(null, query, 0).getContent();
        for (Word w : words) {
            if (count >= LIMIT) break;
            sb.append(w.getSpelling());
            appendPhonetic(sb, w.getPhonetic());
            sb.append(" = ").append(shorten(w.getMeaning()))
                    .append(w.isExtended() ? "（扩展词典）" : "（考试词库）")
                    .append('\n');
            count++;
        }

        // 2. 扩展词典：search 本身已排除 words 表里有的词，不会重复
        if (count < LIMIT) {
            for (DictEntry e : dictionaryService.search(query)) {
                if (count >= LIMIT) break;
                sb.append(e.word());
                appendPhonetic(sb, e.phonetic());
                sb.append(" = ").append(shorten(e.translation()))
                        .append(e.phrase() ? "（扩展词典·短语）" : "（扩展词典）")
                        .append('\n');
                count++;
            }
        }

        if (count == 0) {
            return "词典中没有找到与「" + query.strip() + "」匹配的单词";
        }
        return "找到 " + count + " 个结果（最多显示 " + LIMIT + " 个）：\n" + sb;
    }

    private static void appendPhonetic(StringBuilder sb, String phonetic) {
        if (phonetic != null && !phonetic.isBlank()) {
            sb.append(" [").append(phonetic).append(']');
        }
    }

    // 释义里可能有换行，换成分号，保证一个词占一行
    static String shorten(String meaning) {
        if (meaning == null) {
            return "";
        }
        String oneLine = meaning.replaceAll("\\s*[\\r\\n]+\\s*", "；").strip();
        return oneLine.length() > MAX_MEANING_LENGTH
                ? oneLine.substring(0, MAX_MEANING_LENGTH) + "…"
                : oneLine;
    }
}