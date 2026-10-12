-- 用户名区分大小写：Jay 和 jay 是两个不同的用户
-- MODIFY 会用新定义完整替换旧定义，所以类型、NOT NULL 都要写全
ALTER TABLE users
    MODIFY username VARCHAR(50) NOT NULL COLLATE utf8mb4_bin;

-- 「记住我」令牌表的 username 也必须一起改，
-- 否则 jay 退出登录时会把 Jay 的令牌一起删掉
ALTER TABLE persistent_logins
    MODIFY username VARCHAR(50) NOT NULL COLLATE utf8mb4_bin;