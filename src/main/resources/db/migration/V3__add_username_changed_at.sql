-- 上次修改用户名的时间，用于限制 3 天内只能改一次
-- 从没改过用户名时为 NULL
ALTER TABLE users
    ADD COLUMN username_changed_at DATETIME(6) NULL;