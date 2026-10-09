"""
清理 mysqldump --no-data --compact 导出的表结构，生成 Flyway 的 V1 迁移脚本

用法：
    python scripts/clean_schema_dump.py E:\\schema_raw.sql src/main/resources/db/migration/V1__init_schema.sql
"""
import re
import sys

HEADER = """-- V1：初始表结构
-- 由现有数据库的 mysqldump 导出后清理生成（见 scripts/clean_schema_dump.py）
-- 已执行过的迁移脚本不能再修改，以后的表结构变化请新建 V2、V3……

-- 导出的表按字母顺序排列，建表时外键引用的表可能还不存在，先临时关闭外键检查
SET FOREIGN_KEY_CHECKS = 0;

"""

FOOTER = """

SET FOREIGN_KEY_CHECKS = 1;
"""


def main(src, dst):
    with open(src, encoding="utf-8") as f:
        lines = f.read().splitlines()

    # 去掉 /*!40101 ... */ 这类 MySQL 版本注释行
    lines = [line for line in lines if not line.strip().startswith("/*!")]

    # 保险：去掉 mysqldump 可能附带的会话 / GTID 设置（SET @@GLOBAL.GTID_PURGED 等）
    lines = [line for line in lines if not line.strip().upper().startswith("SET @")]
    text = "\n".join(lines).strip()

    # 去掉表选项里的 AUTO_INCREMENT=数字（列定义里的 AUTO_INCREMENT 后面没有等号，不受影响）
    text = re.sub(r"\s*AUTO_INCREMENT=\d+", "", text)

    # 每个建表语句之间空一行，便于阅读
    text = re.sub(r";\s*CREATE TABLE", ";\n\nCREATE TABLE", text)

    with open(dst, "w", encoding="utf-8", newline="\n") as f:
        f.write(HEADER + text + FOOTER)

    print(f"已生成：{dst}，共 {text.count('CREATE TABLE')} 张表")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print(__doc__)
        sys.exit(1)
    main(sys.argv[1], sys.argv[2])