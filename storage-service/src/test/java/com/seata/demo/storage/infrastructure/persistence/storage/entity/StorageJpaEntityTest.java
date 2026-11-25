package com.seata.demo.storage.infrastructure.persistence.storage.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 庫存 JPA 充血模型實體單元測試 (StorageJpaEntity Test)
 * 驗證充血模型內部業務邏輯、庫存扣減規則、庫存不足例外與不變性守恆
 */
@DisplayName("庫存 JPA 充血模型實體單元測試")
class StorageJpaEntityTest {

    /**
     * Feature: 庫存充血模型工廠方法建立
     * Scenario: 建立新庫存實體時，正確初始化 used 為 0 且 residue 等於 total
     * Given 商品 ID 為 20001，初始總庫存為 100
     * When 呼叫 StorageJpaEntity.create
     * Then 實體正確初始化，各項庫存守恆 (total == used + residue)
     */
    @Test
    @DisplayName("情境：正確建立初始庫存充血實體")
    void givenValidParams_whenCreate_thenSuccess() {
        // Given & When
        StorageJpaEntity storage = StorageJpaEntity.create(20001L, 100);

        // Then
        Assertions.assertNotNull(storage);
        Assertions.assertEquals(20001L, storage.getProductId());
        Assertions.assertEquals(100, storage.getTotal());
        Assertions.assertEquals(0, storage.getUsed());
        Assertions.assertEquals(100, storage.getResidue());
        Assertions.assertTrue(storage.isAvailable(50));
    }

    /**
     * Feature: 庫存扣減業務行為
     * Scenario: 當庫存充足時，成功扣減庫存並維護總量不變性
     * Given 初始庫存為 100
     * When 執行 deduct(20)
     * Then residue 變為 80，used 變為 20，總量不變
     */
    @Test
    @DisplayName("情境：充足庫存下正常扣減")
    void givenSufficientStock_whenDeduct_thenSuccess() {
        // Given
        StorageJpaEntity storage = StorageJpaEntity.create(20001L, 100);

        // When
        storage.deduct(20);

        // Then
        Assertions.assertEquals(80, storage.getResidue());
        Assertions.assertEquals(20, storage.getUsed());
        Assertions.assertEquals(100, storage.getTotal());
    }

    /**
     * Feature: 庫存不足防禦
     * Scenario: 當扣減數量大於剩餘庫存時，拋出 IllegalStateException
     * Given 初始庫存為 10
     * When 執行 deduct(20)
     * Then 拋出 IllegalStateException
     */
    @Test
    @DisplayName("情境：庫存不足時拒絕扣減並拋出例外")
    void givenInsufficientStock_whenDeduct_thenThrowException() {
        // Given
        StorageJpaEntity storage = StorageJpaEntity.create(20001L, 10);

        // When & Then
        Assertions.assertThrows(IllegalStateException.class, () -> storage.deduct(20));
    }

    /**
     * Feature: 庫存業務重置
     * Scenario: 呼叫 reset 重置總庫存
     * Given 庫存已發生過扣減 (used=30, residue=70)
     * When 呼叫 reset(200)
     * Then total 變為 200，used 重置為 0，residue 變為 200
     */
    @Test
    @DisplayName("情境：重置庫存至新總量")
    void givenUsedStock_whenReset_thenSuccess() {
        // Given
        StorageJpaEntity storage = StorageJpaEntity.create(20001L, 100);
        storage.deduct(30);

        // When
        storage.reset(200);

        // Then
        Assertions.assertEquals(200, storage.getTotal());
        Assertions.assertEquals(0, storage.getUsed());
        Assertions.assertEquals(200, storage.getResidue());
    }
}
