-- 登录后弹出「最近一次更新」：记录每个用户看过哪条更新、弹出过几次
-- seen_changelog_id：已弹出过的最新一条更新的编号，从没弹出过时为 NULL
-- changelog_shown_count：这条更新已经弹出的次数，出了新的更新会重新从 0 计数
ALTER TABLE users
    ADD COLUMN seen_changelog_id INT NULL,
    ADD COLUMN changelog_shown_count INT NOT NULL DEFAULT 0;