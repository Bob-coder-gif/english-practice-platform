package com.jay.englishpracticeplatform.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;

//管理所有工具。生成发给模型的说明说，按名分发执行
@Slf4j
@Component
public class ToolRegistry {

    private final Map<String ,Tool> tools;
    private final JsonMapper mapper;
    private final ArrayNode definitions;

    /*
        Spring 会把容器里所有 Tool 类型的 Bean 收集成一个 List 注入进来
        新增一个工具，只需要写一个带 @Component 的实现类，这里不用修改
     */
    public ToolRegistry(List<Tool> toolList,JsonMapper mapper){
        this.mapper = mapper;
        Map<String , Tool> map = new LinkedHashMap<>();
        for(Tool tool: toolList){
            if(map.putIfAbsent(tool.name(),tool) != null){
                //两个工具重名时模型无法区分，启动时直接失败
                throw new IllegalStateException("工具名重复： " + tool.name());
            }
        }
        this.tools = Collections.unmodifiableMap(map);
        this.definitions = buildDefinitions();
        log.info("已注册 {} 个 agent 工具：{}",tools.size(),tools.keySet());
    }

    //请求里的 tools 字段 [{"type":"function","function":{name,description, parameters}},...]
    private ArrayNode buildDefinitions(){
        ArrayNode defs = mapper.createArrayNode();
        for(Tool tool: tools.values()){
            ObjectNode function = defs.addObject()
                    .put("type","function")
                    .putObject("function");
            function.put("name",tool.name());
            function.put("description",tool.description());
            function.set("parameters",tool.parameters());
        }
        return defs;
    }

    /*
    说明书只在第一次启动时生成一次，每次返回一份副本
    Register 是单例，会被多个请求线程同时使用，返回副本保证某个调用方法误改了它。不会影响别人
     */
    public ArrayNode definitions(){
        return definitions.deepCopy();
    }

    // 执行模型请求的工具；任何问题都转成错误文字，不让异常打断 agent 循环
    public String execute(String name,String argumentsJson, ToolContext ctx){
        Tool tool = tools.get(name);
        if(tool == null){
            return "错误：不存在名为 " + name + " 的工具";
        }

        //arguments 是 JSON 字符串，模型偶尔会生成不合法的JSON
        JsonNode args;
        try{
            args = mapper.readTree(argumentsJson == null || argumentsJson.isBlank() ? "{}" : argumentsJson);
        }
        catch (JacksonException e){
            return "错误：参数不是合法的 JSON：" + argumentsJson;
        }

        try{
            return tool.execute(args,ctx);
        }
        catch (RuntimeException e){
            //模型只看简短原因，完整堆栈写入日志供排查
            log.warn("工具 {} 执行失败， 用户 {}， 参数 {}", name,ctx.userId(), argumentsJson, e);
            return "错误：工具执行失败：" + e.getMessage();
        }
    }
}
