package com.seata.demo.order.domain.order.aggregate.vo;

/**
 * 訂單狀態列舉值物件 (Order Status Enum Value Object)
 *
 * @author Seata Architecture Demo Team
 */
public enum OrderStatus {

    /**
     * 初始化中
     */
    INIT,

    /**
     * 下單成功
     */
    SUCCESS,

    /**
     * 下單失敗 (已觸發分散式回滾)
     */
    FAILED
}
