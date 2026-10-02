package com.jay.englishpracticeplatform.dto;

import java.io.Serializable;

// record Java16 引入，专门用来定义只装数据。不可修改的类
// 只存需要的信息。 后续请求只需要知道「当前用户是谁」，id 和用户名就够了。
// Session 存在服务器内存里，每个登录用户各占一份，存得越少越好。
public record LoginUser(Long id, String username) implements Serializable {

}
