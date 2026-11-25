package com.seata.demo.order.infrastructure.persistence.order.entity;

import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 訂單 JPA 充血模型實體單元測試 (OrderJpaEntity Test)
 * 驗證訂單實體工廠建立、狀態機推進與參數校驗
 */
@DisplayName("訂單 JPA 充血模型實體單元測試")
class OrderJpaEntityTest {

    /**
     * Feature: 訂單充血實體工廠建立
     * Scenario: 建立新訂單時，預設狀態為 CREATED 且建立時間不為空
     * Given 合法參數
     * When 呼叫 OrderJpaEntity.create
     * Then 實體成功建立，狀態為 CREATED
     */
    @Test
    @DisplayName("情境：正確建立初始訂單充血實體")
    void givenValidParams_whenCreate_thenSuccess() {
        // Given & When
        OrderJpaEntity order = OrderJpaEntity.create(1001L, 10001L, 20001L, 2, new BigDecimal("500.00"));

        // Then
        Assertions.assertNotNull(order);
        Assertions.assertEquals(1001L, order.getId());
        Assertions.assertEquals(10001L, order.getUserId());
        Assertions.assertEquals(20001L, order.getProductId());
        Assertions.assertEquals(2, order.getCount());
        Assertions.assertEquals(new BigDecimal("500.00"), order.getAmount());
        Assertions.assertEquals("CREATED", order.getStatus());
        Assertions.assertTrue(order.isCreated());
        Assertions.assertNotNull(order.getGmtCreate());
    }

    /**
     * Feature: 訂單生命週期狀態推進
     * Scenario: 訂單完成後推進至 SUCCESS，失敗後推進至 FAILED
     * Given 處於 CREATED 狀態的訂單
     * When 分別呼叫 markAsSuccess 與 markAsFailed
     * Then 狀態正確切換
     */
    @Test
    @DisplayName("情境：訂單生命週期狀態流轉")
    void givenCreatedOrder_whenStatusTransitions_thenSuccess() {
        // Given
        OrderJpaEntity order = OrderJpaEntity.create(1001L, 10001L, 20001L, 1, new BigDecimal("100.00"));

        // When
        order.markAsSuccess();

        // Then
        Assertions.assertTrue(order.isSuccess());
        Assertions.assertFalse(order.isCreated());

        // When
        order.markAsFailed();

        // Then
        Assertions.assertTrue(order.isFailed());
        Assertions.assertFalse(order.isSuccess());
    }

    /**
     * Feature: 訂單參數防禦校驗
     * Scenario: 訂單金額或數量非正數時拒絕建立
     */
    @Test
    @DisplayName("情境：非法參數拒絕建立訂單")
    void givenInvalidParams_whenCreate_thenThrowException() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                OrderJpaEntity.create(null, 10001L, 20001L, 1, new BigDecimal("100.00")));

        Assertions.assertThrows(IllegalArgumentException.class, () ->
                OrderJpaEntity.create(1001L, 10001L, 20001L, 0, new BigDecimal("100.00")));

        Assertions.assertThrows(IllegalArgumentException.class, () ->
                OrderJpaEntity.create(1001L, 10001L, 20001L, 1, BigDecimal.ZERO));
    }
}
