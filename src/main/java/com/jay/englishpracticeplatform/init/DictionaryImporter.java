package com.jay.englishpracticeplatform.init;

import com.jay.englishpracticeplatform.repository.DictionaryRepository;
import com.jay.englishpracticeplatform.repository.DictionaryRepository.DictRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

// 启动时导入扩展词典（数据文件由 scripts/build_dictionary.py 生成）
// 和 VocabularyImporter 一样是幂等的：表里已经有数据就跳过，所以只有第一次启动会导入
@Component
public class DictionaryImporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DictionaryImporter.class);
    private static final String DATA_FILE = "dictionary/dict_entries.tsv.gz";

    // 每条 INSERT 带多少行
    private static final int BATCH_SIZE = 1000;

    private final DictionaryRepository dictionaryRepository;

    public DictionaryImporter(DictionaryRepository dictionaryRepository) {
        this.dictionaryRepository = dictionaryRepository;
    }

    // 整个导入放在一个事务里：中途失败会全部回滚，表还是空的，下次启动重新导入
    // 如果不这样，导入一半失败后表里有数据，以后每次启动都会以为「已经导入过」而跳过
    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (dictionaryRepository.count() > 0) {
            log.info("扩展词典已存在，跳过导入");
            return;
        }

        long start = System.currentTimeMillis();
        int total = 0;
        int inserted = 0;
        List<DictRow> batch = new ArrayList<>(BATCH_SIZE);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new ClassPathResource(DATA_FILE).getInputStream()),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                batch.add(parse(line));
                total++;
                if (batch.size() == BATCH_SIZE) {
                    inserted += dictionaryRepository.insertBatch(batch);
                    batch.clear();
                }
            }
        }
        inserted += dictionaryRepository.insertBatch(batch);

        log.info("扩展词典导入完成：读取 {} 条，写入 {} 条，耗时 {} ms",
                total, inserted, System.currentTimeMillis() - start);
    }

    // 一行 5 列，Tab 分隔：word  phonetic  translation  freq_rank  phrase
    // split 的第二个参数 -1：保留末尾的空列（比如 freq_rank 为空）
    static DictRow parse(String line) {
        String[] cols = line.split("\t", -1);
        if (cols.length != 5) {
            throw new IllegalStateException("扩展词典数据格式错误：" + line);
        }
        return new DictRow(
                cols[0],
                cols[1].isEmpty() ? null : cols[1],
                unescape(cols[2]),
                cols[3].isEmpty() ? null : Integer.valueOf(cols[3]),
                "1".equals(cols[4]));
    }

    // 还原脚本里的转义：\n → 换行，\\ → 反斜杠
    static String unescape(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                char next = text.charAt(++i);
                sb.append(next == 'n' ? '\n' : next);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}