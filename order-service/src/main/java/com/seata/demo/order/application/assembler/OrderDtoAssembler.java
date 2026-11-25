package com.seata.demo.order.application.assembler;

import com.seata.demo.order.application.dto.result.OrderGottenResult;
import com.seata.demo.order.domain.order.aggregate.root.Order;

/**
 * 訂單 DTO 轉換器 (Order DTO Assembler)
 */
public final class OrderDtoAssembler {

    private OrderDtoAssembler() {}

    public static OrderGottenResult toResult(Order order) {
        if (order == null) {
            return null;
        }
        return OrderGottenResult.builder()
                .orderId(order.getId().getValue())
                .userId(order.getUserId().getValue())
                .productId(order.getProductId().getValue())
                .count(order.getCount().getValue())
                .amount(order.getAmount().getValue())
                .status(order.getStatus().name())
                .build();
    }
}
