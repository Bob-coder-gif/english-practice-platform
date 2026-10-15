package com.jay.englishpracticeplatform.repository;

import com.jay.englishpracticeplatform.dto.DictEntry;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class DictionaryRepository {

    private static final String COLUMNS = "d.id, d.word, d.phonetic, d.translation, d.phrase";

    private final JdbcClient jdbcClient;

    public DictionaryRepository(JdbcClient jdbcClient){
        this.jdbcClient = jdbcClient;
    }

    public long count(){
        return jdbcClient.sql("SELECT COUNT(*) FROM dict_entries")
                .query(Long.class)
                .single();
    }

    public Optional<DictEntry> findById(Long id){
        return jdbcClient.sql("SELECT " + COLUMNS + " FROM dict_entries d WHERE d.id = ?")
                .param(id)
                .query(DictEntry.class)
                .optional();
    }

    // 英文：按前缀搜索（能走 word 上的索引）
    // 排序：完全相同的排最前，单词排在短语前，然后按词频，最后按字母
    // 已经在 words 表中的词不显示，它们会出现在「词库中的单词」里
    // LIKE 没有写 ESCAPE：MySQL 默认就用反斜杠转义，和 WordService.escapeLike 一致
    public List<DictEntry> searchByPrefix(String keyword, String prefixPattern, int limit) {
        return jdbcClient.sql("""
                        SELECT %s FROM dict_entries d
                        WHERE d.word LIKE :prefix
                          AND NOT EXISTS (SELECT 1 FROM words w WHERE w.spelling = d.word)
                        ORDER BY d.word = :keyword DESC, d.phrase, d.freq_rank IS NULL, d.freq_rank, d.word
                        LIMIT :limit
                        """.formatted(COLUMNS))
                .param("prefix", prefixPattern)
                .param("keyword", keyword)
                .param("limit", limit)
                .query(DictEntry.class)
                .list();
    }

    // 中文：在释义中查找（%关键词% 用不了索引，要扫全表，9.6 万行在可接受范围内）
    public List<DictEntry> searchByMeaning(String containsPattern, int limit) {
        return jdbcClient.sql("""
                        SELECT %s FROM dict_entries d
                        WHERE d.translation LIKE :contains
                          AND NOT EXISTS (SELECT 1 FROM words w WHERE w.spelling = d.word)
                        ORDER BY d.phrase, d.freq_rank IS NULL, d.freq_rank, d.word
                        LIMIT :limit
                        """.formatted(COLUMNS))
                .param("contains", containsPattern)
                .param("limit", limit)
                .query(DictEntry.class)
                .list();
    }

    // 批量插入：一条 INSERT 带多行 VALUES，比逐行插入快几十倍
    // INSERT IGNORE：万一数据文件里有数据库认为重复的词（比如只有大小写不同），跳过而不是整体失败
    public int insertBatch(List<DictRow> rows) {
        if (rows.isEmpty()) {
            return 0;
        }
        StringBuilder sql = new StringBuilder(
                "INSERT IGNORE INTO dict_entries (word, phonetic, translation, freq_rank, phrase) VALUES ");
        List<Object> params = new ArrayList<>(rows.size() * 5);
        for (int i = 0; i < rows.size(); i++) {
            sql.append(i == 0 ? "(?, ?, ?, ?, ?)" : ", (?, ?, ?, ?, ?)");
            DictRow r = rows.get(i);
            params.add(r.word());
            params.add(r.phonetic());
            params.add(r.translation());
            params.add(r.freqRank());
            params.add(r.phrase());
        }
        return jdbcClient.sql(sql.toString()).params(params).update();
    }

    // 导入时的一行数据
    public record DictRow(String word, String phonetic, String translation, Integer freqRank, boolean phrase) {
    }
}
