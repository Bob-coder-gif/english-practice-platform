package com.jay.englishpracticeplatform.dto;

//扩展词典中的一个词条（dict_entries 表的一行）
public record DictEntry(Long id,String word, String phonetic,String translation, boolean phrase) {

}
