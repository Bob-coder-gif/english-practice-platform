"""
从 ECDICT（https://github.com/skywind3000/ECDICT，MIT License）中
筛选常用单词和短语，生成「扩展词典」数据文件
src/main/resources/dictionary/dict_entries.tsv.gz。

程序第一次启动时，DictionaryImporter 会把这个文件导入 dict_entries 表。

筛选规则：
  单词（不含空格）：满足任意一条
    - 有考试标签（中考、高考、四六级、考研、托福、雅思、GRE）
    - 词频排名在前 50000（frq 或 bnc，取较小的）
    - 有柯林斯星级，或属于牛津 3000 核心词
  短语（含空格）：全小写、不超过 3 个词，并且满足任意一条
    - 有考试标签 / 柯林斯星级 / 牛津 3000
    - 每个词都属于最常用的 2000 个词
  释义为空的词条一律跳过

文件格式：每行一个词条，Tab 分隔，5 列
  word  phonetic  translation  freq_rank  phrase
  translation 中的换行写成 \\n，反斜杠写成 \\\\；freq_rank 为空表示没有词频数据

用法（在项目根目录执行）：
    python scripts/build_dictionary.py <ecdict.csv 的路径>
"""
import csv
import gzip
import sys
from pathlib import Path

OUTPUT = Path("src/main/resources/dictionary/dict_entries.tsv.gz")

WORD_RANK_LIMIT = 50000      # 单词：词频前 5 万
PHRASE_COMMON_LIMIT = 2000   # 短语：每个词都在最常用的 2000 词内
PHRASE_MAX_WORDS = 3

# 和 words 表的列长度保持一致，超长的词条直接跳过，释义超长就截断
MAX_WORD = 50
MAX_PHONETIC = 64
MAX_TRANSLATION = 500


def freq_rank(row):
    """词频排名：frq（现代语料）和 bnc（英国国家语料库）中较小的那个，0 表示没有数据"""
    ranks = [int(row[k]) for k in ("frq", "bnc") if (row[k] or "0").isdigit() and int(row[k]) > 0]
    return min(ranks) if ranks else None


def is_notable(row):
    """有考试标签、柯林斯星级，或属于牛津 3000"""
    return bool((row["tag"] or "").strip()) \
        or (row["collins"] or "0") not in ("", "0") \
        or row["oxford"] == "1"


def clean_translation(text):
    # 和 extract_ecdict.py 一致：ECDICT 中的换行是字面量 "\n"，转成真正的换行，去掉空行
    text = text.replace("\\r\\n", "\n").replace("\\n", "\n")
    lines = [line.strip() for line in text.split("\n")]
    return "\n".join(line for line in lines if line)


def escape(text):
    return text.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", " ")


def main():
    if len(sys.argv) != 2:
        print("用法：python scripts/build_dictionary.py <ecdict.csv 的路径>")
        sys.exit(1)

    with open(sys.argv[1], encoding="utf-8", newline="") as f:
        rows = list(csv.DictReader(f))

    common = {r["word"].lower() for r in rows
              if (freq_rank(r) or 10 ** 9) <= PHRASE_COMMON_LIMIT}

    def keep(r):
        word = r["word"].strip()
        if not word or len(word) > MAX_WORD:
            return False
        if " " not in word:
            return is_notable(r) or (freq_rank(r) or 10 ** 9) <= WORD_RANK_LIMIT
        parts = word.split()
        return word.islower() and len(parts) <= PHRASE_MAX_WORDS \
            and (is_notable(r) or all(p in common for p in parts))

    entries = {}
    for r in rows:
        if not keep(r):
            continue
        translation = clean_translation(r["translation"] or "")[:MAX_TRANSLATION]
        if not translation:
            continue
        word = r["word"].strip()
        # 数据库按不区分大小写比较，这里也按小写去重，保留先出现的
        key = word.lower()
        if key in entries:
            continue
        phonetic = (r["phonetic"] or "").strip()[:MAX_PHONETIC]
        rank = freq_rank(r)
        entries[key] = (word, phonetic, translation, "" if rank is None else str(rank),
                        "1" if " " in word else "0")

    lines = ["\t".join([e[0], e[1], escape(e[2]), e[3], e[4]])
             for e in sorted(entries.values(), key=lambda e: (e[0].lower(), e[0]))]

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    # mtime=0：内容不变时生成的文件也完全一样，Git 不会认为它改动了
    with gzip.GzipFile(OUTPUT, "wb", mtime=0) as gz:
        gz.write(("\n".join(lines) + "\n").encode("utf-8"))

    phrases = sum(1 for e in entries.values() if e[4] == "1")
    print(f"共 {len(entries)} 个词条（单词 {len(entries) - phrases}，短语 {phrases}），已写入 {OUTPUT}")
    print(f"文件大小 {OUTPUT.stat().st_size / 1024 / 1024:.1f} MB")


if __name__ == "__main__":
    main()