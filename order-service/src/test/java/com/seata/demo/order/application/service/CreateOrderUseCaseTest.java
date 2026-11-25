package com.seata.demo.order.application.service;

import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.seata.demo.order.application.dto.command.CreateOrderCommand;
import com.seata.demo.order.application.dto.result.OrderGottenResult;
import com.seata.demo.order.application.port.out.AccountClientPort;
import com.seata.demo.order.application.port.out.IdGeneratorPort;
import com.seata.demo.order.application.port.out.OrderRepositoryPort;
import com.seata.demo.order.application.port.out.StorageClientPort;
import com.seata.demo.order.domain.order.aggregate.root.Order;

/**
 * 建立訂單案例單元測試 (Create Order UseCase Unit Test)
 * 遵循規範 5: 使用 BDD 與 Gherkin 語法撰寫測試描述，涵蓋正常路徑與模擬異常路徑。
 *
 */
@ExtendWith(MockitoExtension.class)
class CreateOrderUseCaseTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @Mock
    private StorageClientPort storageClientPort;

    @Mock
    private AccountClientPort accountClientPort;

    @Mock
    private IdGeneratorPort idGeneratorPort;

    private OrderApplicationService orderApplicationService;

    @BeforeEach
    void setUp() {
        orderApplicationService = new OrderApplicationService(
                orderRepositoryPort,
                storageClientPort,
                accountClientPort,
                idGeneratorPort);
    }

    /**
     * Feature: 建立訂單與全鏈路調用 (Happy Path)
     * Scenario: 當庫存與金額均正常時，成功建立訂單
     * Given 用戶 ID 10001，商品 ID 20001，購買數量 2，支付金額 500.00
     * When 執行下單操作
     * Then 依序調用庫存扣減、調用帳戶凍結，並持久化訂單且狀態為 SUCCESS
     */
    @Test
    @DisplayName("情境：正常流程下成功建立訂單")
    void givenValidOrder_whenCreateOrder_thenSuccess() {
        // Given
        CreateOrderCommand command = CreateOrderCommand.builder()
                .userId(10001L)
                .productId(20001L)
                .count(2)
                .payAmount(new BigDecimal("500.00"))
                .simulateError(false)
                .build();

        Mockito.when(idGeneratorPort.nextId()).thenReturn(999001L);
        Mockito.when(orderRepositoryPort.save(Mockito.any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        OrderGottenResult result = orderApplicationService.createOrder(command);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(999001L, result.getOrderId());
        Assertions.assertEquals(10001L, result.getUserId());
        Assertions.assertEquals(20001L, result.getProductId());
        Assertions.assertEquals(2, result.getCount());
        Assertions.assertEquals(new BigDecimal("500.00"), result.getAmount());
        Assertions.assertEquals("SUCCESS", result.getStatus());

        Mockito.verify(idGeneratorPort, Mockito.times(1)).nextId();
        Mockito.verify(storageClientPort, Mockito.times(1)).deductStorage(20001L, 2);
        Mockito.verify(accountClientPort, Mockito.times(1)).preparePayment(10001L, new BigDecimal("500.00"));
        Mockito.verify(orderRepositoryPort, Mockito.times(1)).save(Mockito.any(Order.class));
    }

    /**
     * Feature: 建立訂單與全鏈路調用 (Simulate Error / Rollback)
     * Scenario: 當開啟 simulateError=true 時，持久化後拋出 RuntimeException 觸發回滾
     * Given 下單請求帶有 simulateError = true
     * When 執行下單操作
     * Then 系統拋出 RuntimeException，阻止方法正常完成，以便觸發 Seata 全局回滾
     */
    @Test
    @DisplayName("情境：人為模擬異常時拋出 RuntimeException 觸發全局回滾")
    void givenSimulateError_whenCreateOrder_thenThrowException() {
        // Given
        CreateOrderCommand command = CreateOrderCommand.builder()
                .userId(10001L)
                .productId(20001L)
                .count(2)
                .payAmount(new BigDecimal("500.00"))
                .simulateError(true)
                .build();

        Mockito.when(idGeneratorPort.nextId()).thenReturn(999002L);
        Mockito.when(orderRepositoryPort.save(Mockito.any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When & Then
        Assertions.assertThrows(RuntimeException.class, () -> {
            orderApplicationService.createOrder(command);
        });

        Mockito.verify(idGeneratorPort, Mockito.times(1)).nextId();
        Mockito.verify(storageClientPort, Mockito.times(1)).deductStorage(20001L, 2);
        Mockito.verify(accountClientPort, Mockito.times(1)).preparePayment(10001L, new BigDecimal("500.00"));
        Mockito.verify(orderRepositoryPort, Mockito.times(1)).save(Mockito.any(Order.class));
    }
}
