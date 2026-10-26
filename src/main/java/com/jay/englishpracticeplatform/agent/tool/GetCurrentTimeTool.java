package com.jay.englishpracticeplatform.agent.tool;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class GetCurrentTimeTool implements Tool {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss EEEE", Locale.CHINA);

    private final Clock clock;

    /** Spring 用这个：有多个构造器时，Spring 默认选择无参构造器 */
    public GetCurrentTimeTool() {
        this(Clock.system(ZoneId.of("Asia/Shanghai")));
    }

    /** 测试用：传入固定时钟，结果可预测 */
    GetCurrentTimeTool(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String name() {
        return "get_current_time";
    }

    @Override
    public String description() {
        return "获取当前的日期和时间（北京时间）。用户问到今天几号、星期几、现在几点等与当前时间有关的问题时使用。";
    }

    @Override
    public ObjectNode parameters() {
        return Tool.noParams();
    }

    @Override
    public String execute(JsonNode args, ToolContext ctx) {
        return ZonedDateTime.now(clock).format(FORMAT);
    }
}