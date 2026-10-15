-- V6：扩展词典
-- 1. dict_entries：从 ECDICT 筛选的约 9.6 万个单词和短语，只用来搜索；数据由 DictionaryImporter 在启动时导入
-- 2. words.source：区分单词来源。CORE = 原来的考试词库，EXTENDED = 用户从扩展词典收藏进来的

CREATE TABLE dict_entries (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    word        VARCHAR(50)  NOT NULL,
    phonetic    VARCHAR(64)  NULL,
    translation VARCHAR(500) NOT NULL,
    freq_rank   INT          NULL COMMENT '词频排名，越小越常用；NULL 表示没有词频数据',
    phrase      BIT(1)       NOT NULL COMMENT '是否是短语（含空格）',
    PRIMARY KEY (id),
    -- 唯一索引同时用来做前缀搜索：word LIKE 'app%' 可以走这个索引
    UNIQUE KEY uk_dict_entries_word (word)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 已有的单词全部是考试词库中的，默认值 CORE 正好适用
ALTER TABLE words
    ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'CORE';