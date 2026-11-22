package com.seata.demo.storage.application.service;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.seata.demo.storage.application.dto.query.GetStorageQuery;
import com.seata.demo.storage.application.dto.result.StorageGottenResult;
import com.seata.demo.storage.application.port.out.StorageRepositoryPort;
import com.seata.demo.storage.domain.storage.aggregate.root.Storage;
import com.seata.demo.storage.domain.storage.aggregate.vo.ProductId;
import com.seata.demo.storage.domain.storage.aggregate.vo.Quantity;
import com.seata.demo.storage.domain.storage.aggregate.vo.StorageId;
import com.seata.demo.storage.domain.storage.exception.StorageNotFoundException;

/**
 * 查詢商品庫存業務案例單元測試 (Get Storage UseCase Unit Test)
 * 遵循規範 5: 使用 BDD 與 Gherkin 語法撰寫測試描述，涵蓋正常查詢與查無記錄之異常分支。
 *
 * @author Seata Architecture Demo Team
 */
@ExtendWith(MockitoExtension.class)
class GetStorageUseCaseTest {

    @Mock
    private StorageRepositoryPort storageRepositoryPort;

    private StorageApplicationService storageApplicationService;

    @BeforeEach
    void setUp() {
        storageApplicationService = new StorageApplicationService(storageRepositoryPort);
    }

    /**
     * Feature: 商品庫存業務查詢
     * Scenario: 當商品存在時，成功取得即時庫存詳情
     * Given 商品 ID 為 20001，總庫存 100，已用庫存 2，剩餘庫存 98
     * When 接收到依據商品 ID 查詢庫存之 Query
     * Then 成功返回對應之 StorageGottenResult，數據完全一致
     */
    @Test
    @DisplayName("情境：商品存在時正常取得庫存詳情")
    void givenExistingProduct_whenGetStorage_thenSuccess() {
        // Given
        Long productId = 20001L;
        Storage storage = new Storage(
                StorageId.of(1L),
                ProductId.of(productId),
                Quantity.of(100),
                Quantity.of(2),
                Quantity.of(98)
        );

        Mockito.when(storageRepositoryPort.findByProductId(ProductId.of(productId)))
                .thenReturn(Optional.of(storage));

        GetStorageQuery query = GetStorageQuery.builder().productId(productId).build();

        // When
        StorageGottenResult result = storageApplicationService.getStorage(query);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(productId, result.getProductId());
        Assertions.assertEquals(100, result.getTotal());
        Assertions.assertEquals(2, result.getUsed());
        Assertions.assertEquals(98, result.getResidue());
        Mockito.verify(storageRepositoryPort, Mockito.times(1)).findByProductId(ProductId.of(productId));
    }

    /**
     * Feature: 商品庫存業務查詢
     * Scenario: 當商品不存在時，拋出 StorageNotFoundException 領域例外
     * Given 商品 ID 為 99999，底層無此記錄
     * When 接收到依據商品 ID 查詢庫存之 Query
     * Then 拋出 StorageNotFoundException
     */
    @Test
    @DisplayName("情境：商品不存在時拋出 StorageNotFoundException")
    void givenNonExistingProduct_whenGetStorage_thenThrowException() {
        // Given
        Long productId = 99999L;
        Mockito.when(storageRepositoryPort.findByProductId(ProductId.of(productId)))
                .thenReturn(Optional.empty());

        GetStorageQuery query = GetStorageQuery.builder().productId(productId).build();

        // When & Then
        Assertions.assertThrows(StorageNotFoundException.class, () -> {
            storageApplicationService.getStorage(query);
        });
        Mockito.verify(storageRepositoryPort, Mockito.times(1)).findByProductId(ProductId.of(productId));
    }
}
