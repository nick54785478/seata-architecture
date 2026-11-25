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

import com.seata.demo.storage.application.dto.command.DeductStorageCommand;
import com.seata.demo.storage.application.port.out.StorageRepositoryPort;
import com.seata.demo.storage.domain.storage.aggregate.root.Storage;
import com.seata.demo.storage.domain.storage.aggregate.vo.ProductId;
import com.seata.demo.storage.domain.storage.aggregate.vo.Quantity;
import com.seata.demo.storage.domain.storage.aggregate.vo.StorageId;
import com.seata.demo.storage.domain.storage.exception.StockInsufficientException;

/**
 * 扣減庫存案例單元測試 (Deduct Storage UseCase Unit Test)
 * 遵循規範 5: 使用 BDD 與 Gherkin 語法撰寫測試描述，涵蓋正常路徑與異常路徑。
 *
 */
@ExtendWith(MockitoExtension.class)
class DeductStorageUseCaseTest {

        @Mock
        private StorageRepositoryPort storageRepositoryPort;

        private StorageApplicationService storageApplicationService;

        @BeforeEach
        void setUp() {
                storageApplicationService = new StorageApplicationService(storageRepositoryPort);
        }

        /**
         * Feature: 商品庫存扣減業務
         * Scenario: 當庫存充足時，成功扣減庫存
         * Given 商品 ID 為 20001，當前剩餘庫存為 100
         * When 接收到扣減 10 件庫存之命令
         * Then 庫存成功被扣減，剩餘庫存變更為 90，已用庫存增加為 10，並成功持久化
         */
        @Test
        @DisplayName("情境：庫存充足時正常扣減成功")
        void givenSufficientStock_whenDeduct_thenSuccess() {
                // Given
                Long productId = 20001L;
                Storage initialStorage = new Storage(
                                StorageId.of(1L),
                                ProductId.of(productId),
                                Quantity.of(100),
                                Quantity.of(0),
                                Quantity.of(100));

                Mockito.when(storageRepositoryPort.findByProductId(ProductId.of(productId)))
                                .thenReturn(Optional.of(initialStorage));
                Mockito.when(storageRepositoryPort.save(Mockito.any(Storage.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                DeductStorageCommand command = DeductStorageCommand.builder()
                                .productId(productId)
                                .count(10)
                                .build();

                // When
                storageApplicationService.deductStorage(command);

                // Then
                Assertions.assertEquals(90, initialStorage.getResidue().getValue());
                Assertions.assertEquals(10, initialStorage.getUsed().getValue());
                Mockito.verify(storageRepositoryPort, Mockito.times(1)).save(initialStorage);
        }

        /**
         * Feature: 商品庫存扣減業務
         * Scenario: 當剩餘庫存不足時，拋出領域例外並阻止扣減
         * Given 商品 ID 為 20001，當前剩餘庫存為 5
         * When 接收到扣減 10 件庫存之命令
         * Then 系統拋出 StockInsufficientException 領域例外，且不調用 save 保存
         */
        @Test
        @DisplayName("情境：剩餘庫存不足時拋出 StockInsufficientException")
        void givenInsufficientStock_whenDeduct_thenThrowException() {
                // Given
                Long productId = 20001L;
                Storage initialStorage = new Storage(
                                StorageId.of(1L),
                                ProductId.of(productId),
                                Quantity.of(100),
                                Quantity.of(95),
                                Quantity.of(5));

                Mockito.when(storageRepositoryPort.findByProductId(ProductId.of(productId)))
                                .thenReturn(Optional.of(initialStorage));

                DeductStorageCommand command = DeductStorageCommand.builder()
                                .productId(productId)
                                .count(10)
                                .build();

                // When & Then
                Assertions.assertThrows(StockInsufficientException.class, () -> {
                        storageApplicationService.deductStorage(command);
                });

                Mockito.verify(storageRepositoryPort, Mockito.never()).save(Mockito.any(Storage.class));
        }
}
