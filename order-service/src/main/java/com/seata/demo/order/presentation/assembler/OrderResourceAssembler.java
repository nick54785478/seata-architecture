package com.seata.demo.order.presentation.assembler;

import com.seata.demo.order.application.dto.command.CreateOrderCommand;
import com.seata.demo.order.application.dto.result.OrderGottenResult;
import com.seata.demo.order.presentation.resource.in.CreateOrderResource;
import com.seata.demo.order.presentation.resource.out.OrderCreatedResource;
import com.seata.demo.order.presentation.resource.out.OrderRetrievedResource;

/**
 * 訂單表現層資源轉換器 (Order Resource Assembler)
 *
 * @author Seata Architecture Demo Team
 */
public final class OrderResourceAssembler {

    private OrderResourceAssembler() {}

    public static CreateOrderCommand toCommand(CreateOrderResource resource) {
        if (resource == null) {
            return null;
        }
        return CreateOrderCommand.builder()
                .userId(resource.getUserId())
                .productId(resource.getProductId())
                .count(resource.getCount())
                .payAmount(resource.getPayAmount())
                .simulateError(resource.getSimulateError())
                .build();
    }

    public static OrderCreatedResource toResource(OrderGottenResult result) {
        if (result == null) {
            return null;
        }
        return OrderCreatedResource.builder()
                .orderId(result.getOrderId())
                .userId(result.getUserId())
                .productId(result.getProductId())
                .count(result.getCount())
                .amount(result.getAmount())
                .status(result.getStatus())
                .build();
    }

    public static OrderRetrievedResource toRetrievedResource(OrderGottenResult result) {
        if (result == null) {
            return null;
        }
        return OrderRetrievedResource.builder()
                .orderId(result.getOrderId())
                .userId(result.getUserId())
                .productId(result.getProductId())
                .count(result.getCount())
                .amount(result.getAmount())
                .status(result.getStatus())
                .build();
    }
}
