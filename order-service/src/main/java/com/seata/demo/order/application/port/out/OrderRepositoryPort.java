package com.seata.demo.order.application.port.out;

import java.util.Optional;

import com.seata.demo.order.domain.order.aggregate.root.Order;
import com.seata.demo.order.domain.order.aggregate.vo.OrderId;

/**
 * 訂單儲存庫 Outbound Port 介面 (Order Repository Port)
 * 命名規範 3-8: ... + Port
 */
public interface OrderRepositoryPort {

    Optional<Order> findById(OrderId orderId);

    Order save(Order order);
}
