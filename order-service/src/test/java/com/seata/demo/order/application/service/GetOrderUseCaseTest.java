package com.seata.demo.order.application.service;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.seata.demo.order.application.dto.query.GetOrderQuery;
import com.seata.demo.order.application.dto.result.OrderGottenResult;
import com.seata.demo.order.application.port.out.AccountClientPort;
import com.seata.demo.order.application.port.out.IdGeneratorPort;
import com.seata.demo.order.application.port.out.OrderRepositoryPort;
import com.seata.demo.order.application.port.out.StorageClientPort;
import com.seata.demo.order.domain.order.aggregate.root.Order;
import com.seata.demo.order.domain.order.aggregate.vo.Money;
import com.seata.demo.order.domain.order.aggregate.vo.OrderId;
import com.seata.demo.order.domain.order.aggregate.vo.ProductId;
import com.seata.demo.order.domain.order.aggregate.vo.Quantity;
import com.seata.demo.order.domain.order.aggregate.vo.UserId;
import com.seata.demo.order.domain.order.exception.OrderNotFoundException;

/**
 * 查詢訂單業務案例單元測試 (Get Order UseCase Unit Test)
 * 遵循規範 5: 使用 BDD 與 Gherkin 語法撰寫測試描述，涵蓋正常查詢與查無訂單之異常分支。
 *
 * @author Seata Architecture Demo Team
 */
@ExtendWith(MockitoExtension.class)
class GetOrderUseCaseTest {

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
                idGeneratorPort
        );
    }

    /**
     * Feature: 訂單業務查詢
     * Scenario: 當訂單存在時，成功取得訂單詳情
     * Given 訂單 ID 為 999001，用戶 10001，商品 20001，數量 2，金額 500.00
     * When 接收到依據訂單 ID 查詢訂單之 Query
     * Then 成功返回對應之 OrderGottenResult，數據完全一致
     */
    @Test
    @DisplayName("情境：訂單存在時正常取得訂單詳情")
    void givenExistingOrder_whenGetOrder_thenSuccess() {
        // Given
        Long orderId = 999001L;
        Order order = Order.create(
                OrderId.of(orderId),
                UserId.of(10001L),
                ProductId.of(20001L),
                Quantity.of(2),
                Money.of(new BigDecimal("500.00"))
        );

        Mockito.when(orderRepositoryPort.findById(OrderId.of(orderId)))
                .thenReturn(Optional.of(order));

        GetOrderQuery query = GetOrderQuery.builder().orderId(orderId).build();

        // When
        OrderGottenResult result = orderApplicationService.getOrder(query);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(orderId, result.getOrderId());
        Assertions.assertEquals(10001L, result.getUserId());
        Assertions.assertEquals(20001L, result.getProductId());
        Assertions.assertEquals(2, result.getCount());
        Assertions.assertEquals(new BigDecimal("500.00"), result.getAmount());
        Assertions.assertEquals("SUCCESS", result.getStatus());

        Mockito.verify(orderRepositoryPort, Mockito.times(1)).findById(OrderId.of(orderId));
    }

    /**
     * Feature: 訂單業務查詢
     * Scenario: 當訂單不存在時，拋出 OrderNotFoundException 領域例外
     * Given 訂單 ID 為 888888，底層無此記錄
     * When 接收到依據訂單 ID 查詢訂單之 Query
     * Then 拋出 OrderNotFoundException
     */
    @Test
    @DisplayName("情境：訂單不存在時拋出 OrderNotFoundException")
    void givenNonExistingOrder_whenGetOrder_thenThrowException() {
        // Given
        Long orderId = 888888L;
        Mockito.when(orderRepositoryPort.findById(OrderId.of(orderId)))
                .thenReturn(Optional.empty());

        GetOrderQuery query = GetOrderQuery.builder().orderId(orderId).build();

        // When & Then
        Assertions.assertThrows(OrderNotFoundException.class, () -> {
            orderApplicationService.getOrder(query);
        });

        Mockito.verify(orderRepositoryPort, Mockito.times(1)).findById(OrderId.of(orderId));
    }
}
