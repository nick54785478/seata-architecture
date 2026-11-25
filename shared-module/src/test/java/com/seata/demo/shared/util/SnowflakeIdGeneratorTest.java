package com.seata.demo.shared.util;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 雪花演算法單元測試 (Snowflake ID Generator Test)
 * 遵循規範 5: 使用 BDD 與 Gherkin 語法撰寫測試描述。
 *
 */
class SnowflakeIdGeneratorTest {

    /**
     * Feature: 分散式雪花演算法生成唯一 ID
     * Scenario: 連續生成 10,000 個 ID，驗證皆為正數且不重複、單調遞增
     * Given 初始化雪花演算法生成器 (workerId=1, datacenterId=1)
     * When 在短時間內連續呼叫 nextId() 生成 10,000 個 ID
     * Then 每個 ID 均大於 0，且所有產生的 ID 互不重複
     */
    @Test
    @DisplayName("情境：大量生成 ID 時確保無重複且單調遞增")
    void givenSnowflakeGenerator_whenGenerateMultipleIds_thenUniqueAndPositive() {
        // Given
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        int count = 10000;
        Set<Long> ids = new HashSet<>(count);

        // When
        long lastId = 0L;
        for (int i = 0; i < count; i++) {
            long currentId = generator.nextId();
            Assertions.assertTrue(currentId > 0, "ID 必須為正數");
            Assertions.assertTrue(currentId > lastId, "ID 必須嚴格單調遞增");
            ids.add(currentId);
            lastId = currentId;
        }

        // Then
        Assertions.assertEquals(count, ids.size(), "所有生成的 ID 必須完全唯一");
    }

    /**
     * Feature: 雪花演算法自適應建構
     * Scenario: 使用無參建構子，由本機網卡與進程號自動推算節點 ID
     * Given 呼叫無參建構子建立生成器
     * When 生成下一個 ID
     * Then 成功回傳大於 0 的合法 ID
     */
    @Test
    @DisplayName("情境：自適應無參建構成功產生合法 ID")
    void givenDefaultConstructor_whenNextId_thenSuccess() {
        // Given
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator();

        // When
        long id = generator.nextId();

        // Then
        Assertions.assertTrue(id > 0);
    }
}
