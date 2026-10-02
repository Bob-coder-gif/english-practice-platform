package com.jay.englishpracticeplatform.init;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.repository.WordRepository;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Component
public class VocabularyImporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(VocabularyImporter.class);
    private static final String WORDS_FILE = "vocabulary/words.json";

    private final WordRepository wordRepository;
    private final JsonMapper jsonMapper;

    public VocabularyImporter(WordRepository wordRepository, JsonMapper jsonMapper){
        this.wordRepository = wordRepository;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws  IOException{
        //幂等，已经导入过就跳过，保证无论启动多少次，结果都一样
        if(wordRepository.count() > 0){
            log.info("词库已存在({}个单词，跳过导入",wordRepository.count());
            return;
        }

        long start = System.currentTimeMillis();

        List<RawWord> rawWords;
        try(InputStream in = new ClassPathResource(WORDS_FILE).getInputStream()){
            rawWords = jsonMapper.readValue(in, new TypeReference<List<RawWord>>(){});

        }

        List<Word> words = toWords(rawWords);
        wordRepository.saveAll(words);

        log.info("词库导入完成，{} 个单词，耗时 {} ms",words.size(), System.currentTimeMillis() - start);
        for(WordLevel level : WordLevel.values()){
            log.info(" {} ({}) ,{} 个",level,level.getLabel(),wordRepository.countByLevel(level));
        }

    }

    private List<Word> toWords(List<RawWord> rawWords) {
        // 防御性去重，数据文件目前没有发现重复，但以后重新生成是不一定
        Map<String, Word> unique = new LinkedHashMap<>();

        for (RawWord raw : rawWords) {
            // 检查是否为 null
            if (raw.word() == null || raw.translation() == null
                    || raw.levels() == null || raw.levels().isEmpty()) {
                continue;
            }

            // 去掉首尾空白
            String spelling = raw.word().strip();
            String meaning = raw.translation().strip();
            // 检查是否为空字符串
            if (spelling.isEmpty() || meaning.isEmpty()) {
                continue;
            }

            String key = spelling.toLowerCase(Locale.ROOT);
            if (unique.containsKey(key)) {
                log.warn("发现重复单词，已跳过 {}", spelling);
                continue;
            }

            Word word = new Word();
            word.setSpelling(spelling);
            word.setPhonetic(raw.phonetic() == null ? null : raw.phonetic().strip());
            word.setMeanings(meaning);
            word.getLevels().addAll(raw.levels);
            unique.put(key,word);
        }

        return new ArrayList<>(unique.values());
    }

    @JsonIgnoreProperties
    record RawWord(String word,String phonetic, String translation, List<WordLevel> levels){

    }
}
