package com.jay.englishpracticeplatform.agent.tool;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/*
    一个工具 = 给模型看的说明说（name，description，parameters）+ 给Java 执行的实现（execute）。
    实现类加上 @Component，ToolRegistry，会自动收集，不需要手动注册
 */
public interface Tool {

    //工具名
    String name();

    //工具描述
    String description();

    //参数的 JSON Schema
    ObjectNode parameters();

    /*
        执行工具
        args 来自模型，不可信，要校验。ctx 由代码提供，可信。
        业务上的失败返回“错误：......" 文字，让模型读懂后自己调整
     */
    String execute(JsonNode args, ToolContext ctx);


    //生成参数 Schema 的小工具

    // 无参数{"type":"object","properties":{}}
    static ObjectNode noParams(){
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type","object");
        schema.putObject("properties");
        return schema;
    }

    //一个必填的字符串参数
    static ObjectNode oneStringParam(String paramName, String paramDescription){
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type","object");
        schema.putObject("properties")
                .putObject(paramName)
                .put("type","string")
                .put("description",paramDescription);
        schema.putArray("required").add(paramName);
        return schema;
    }
}
