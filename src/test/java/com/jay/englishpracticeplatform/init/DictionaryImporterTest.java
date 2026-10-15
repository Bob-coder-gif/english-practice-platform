package com.jay.englishpracticeplatform.init;

import com.jay.englishpracticeplatform.repository.DictionaryRepository.DictRow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// 解析数据文件的一行：纯 Java 逻辑，不需要启动 Spring
// 放在 init 包里，才能调用包级私有的 parse 和 unescape
class DictionaryImporterTest {

    @Test
    void parsesAllColumns() {
        DictRow row = DictionaryImporter.parse("abandon\tә'bændәn\tvt. 放弃\\nn. 放任\t2057\t0");

        assertEquals("abandon", row.word());
        assertEquals("ә'bændәn", row.phonetic());
        assertEquals("vt. 放弃\nn. 放任", row.translation());
        assertEquals(2057, row.freqRank());
        assertFalse(row.phrase());
    }

    @Test
    void emptyColumnsBecomeNull() {
        DictRow row = DictionaryImporter.parse("give up\t\t放弃\t\t1");

        assertNull(row.phonetic());
        assertNull(row.freqRank());
        assertTrue(row.phrase());
    }

    @Test
    void unescapeHandlesBackslash() {
        assertEquals("a\\b\nc", DictionaryImporter.unescape("a\\\\b\\nc"));
    }

    @Test
    void wrongColumnCountThrows() {
        assertThrows(IllegalStateException.class, () -> DictionaryImporter.parse("only\ttwo"));
    }
}