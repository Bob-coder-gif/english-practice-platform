-- AI 助手，每日人每天的提问次数：每日限制
-- 每个用户每天最多一行，次数在这一行上累加
CREATE TABLE agent_usage
(
    user_id         BIGINT  NOT NULL,
    usage_date      DATE    NOT NULL,
    request_count   INT     NOT NULL DEFAULT 0,
    -- 主键 （user_id,usage_date): 按用户和日期定位到唯一一行
    PRIMARY KEY (user_id,usage_date),
    -- 统计全站当天总次数时按日期查询
    KEY idx_agent_usage_date(usage_date),
    CONSTRAINT fk_agent_usage_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;