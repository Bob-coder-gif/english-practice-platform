-- 我的词库：记录「哪个用户收藏了哪个单词」，不复制单词本身
CREATE TABLE user_collections
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    word_id    BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    -- 同一个用户不能重复收藏同一个单词；也用于按用户查询（最左前缀 user_id）
    UNIQUE KEY uk_user_collections_user_word (user_id, word_id),
    -- 外键 word_id 需要索引
    KEY idx_user_collections_word (word_id),
    CONSTRAINT fk_user_collections_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_collections_word FOREIGN KEY (word_id) REFERENCES words (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;