"""
从 ECDICT（https://github.com/skywind3000/ECDICT，MIT License）中
提取高考、四级、六级、考研、雅思五个考试大纲的单词，
生成项目使用的精简词库 src/main/resources/vocabulary/words.json。

用法（在项目根目录执行）：
    python scripts/extract_ecdict.py <ecdict.csv 的路径>
"""
import csv
import json
import sys
from pathlib import Path

# ECDICT 的 tag 字段 → 项目中 WordLevel 枚举的名字
TAG_TO_LEVEL = {
    "gk": "GAOKAO",
    "cet4": "CET4",
    "cet6": "CET6",
    "ky": "KAOYAN",
    "ielts": "IELTS",
}

OUTPUT = Path("src/main/resources/vocabulary/words.json")


def clean_translation(text: str) -> str:
    # ECDICT 中换行是以字面量 "\n" / "\r\n" 存储的，这里转换成真正的换行
    text = text.replace("\\r\\n", "\n").replace("\\n", "\n")
    lines = [line.strip() for line in text.split("\n")]
    return "\n".join(line for line in lines if line)


def main() -> None:
    if len(sys.argv) != 2:
        print("用法：python scripts/extract_ecdict.py <ecdict.csv 的路径>")
        sys.exit(1)

    words = []
    with open(sys.argv[1], encoding="utf-8", newline="") as f:
        for row in csv.DictReader(f):
            tags = (row["tag"] or "").split()
            levels = [TAG_TO_LEVEL[t] for t in tags if t in TAG_TO_LEVEL]
            if not levels:
                continue

            word = row["word"].strip()
            translation = clean_translation(row["translation"] or "")
            if not word or not translation:
                continue

            words.append({
                "word": word,
                "phonetic": (row["phonetic"] or "").strip() or None,
                "translation": translation,
                "levels": levels,
            })

    words.sort(key=lambda w: (w["word"].lower(), w["word"]))

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with open(OUTPUT, "w", encoding="utf-8") as f:
        json.dump(words, f, ensure_ascii=False, indent=1)

    counts = {level: 0 for level in TAG_TO_LEVEL.values()}
    for w in words:
        for level in w["levels"]:
            counts[level] += 1
    print(f"共 {len(words)} 个单词，已写入 {OUTPUT}")
    for level, n in counts.items():
        print(f"  {level}: {n}")


if __name__ == "__main__":
    main()