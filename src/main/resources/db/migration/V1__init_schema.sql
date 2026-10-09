-- V1：初始表结构
-- 由现有数据库的 mysqldump 导出后清理生成（见 scripts/clean_schema_dump.py）
-- 已执行过的迁移脚本不能再修改，以后的表结构变化请新建 V2、V3……

-- 导出的表按字母顺序排列，建表时外键引用的表可能还不存在，先临时关闭外键检查
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `answer_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `answered_at` datetime(6) NOT NULL,
  `correct` bit(1) NOT NULL,
  `mode` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `word_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_answer_records_user_answered_at` (`user_id`,`answered_at`),
  KEY `FKbnceclcah7wakjwnd12ok06jo` (`word_id`),
  CONSTRAINT `FKbnceclcah7wakjwnd12ok06jo` FOREIGN KEY (`word_id`) REFERENCES `words` (`id`),
  CONSTRAINT `FKjpqgu8y6wgnvkn5110k9ycsx1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `answer_records_chk_1` CHECK ((`mode` in (_utf8mb4'LEARN',_utf8mb4'REVIEW',_utf8mb4'DICTATION_EN',_utf8mb4'DICTATION_CN')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `check_ins` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `check_date` date NOT NULL,
  `create_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_check_ins_user_date` (`user_id`,`check_date`),
  CONSTRAINT `FKjj9r2mr1v45h3867kcn28vd7b` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `persistent_logins` (
  `username` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `series` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `token` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `last_used` timestamp NOT NULL,
  PRIMARY KEY (`series`),
  KEY `idx_persistent_logins_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_words` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `known_count` int NOT NULL,
  `last_reviewed_at` datetime(6) NOT NULL,
  `next_review_at` datetime(6) NOT NULL,
  `streak` int NOT NULL,
  `unknown_count` int NOT NULL,
  `user_id` bigint NOT NULL,
  `word_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_words_user_word` (`user_id`,`word_id`),
  KEY `idx_user_words_user_next_review` (`user_id`,`next_review_at`),
  KEY `FK5ip5cm34fpih9occ8v38ur2l6` (`word_id`),
  CONSTRAINT `FK5ip5cm34fpih9occ8v38ur2l6` FOREIGN KEY (`word_id`) REFERENCES `words` (`id`),
  CONSTRAINT `FKnmok7ci9149qjor41u2di237k` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `password_hash` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `daily_goal` int NOT NULL DEFAULT '20',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `word_levels` (
  `word_id` bigint NOT NULL,
  `level` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`word_id`,`level`),
  KEY `idx_word_levels_level` (`level`,`word_id`),
  CONSTRAINT `FKj2kyikv4xu78u226rbaa08jvy` FOREIGN KEY (`word_id`) REFERENCES `words` (`id`),
  CONSTRAINT `word_levels_chk_1` CHECK ((`level` in (_utf8mb4'GAOKAO',_utf8mb4'CET4',_utf8mb4'CET6',_utf8mb4'KAOYAN',_utf8mb4'IELTS')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `words` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `meaning` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phonetic` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `spelling` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK16fbftrxp6e9xvcbxlel9kues` (`spelling`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
