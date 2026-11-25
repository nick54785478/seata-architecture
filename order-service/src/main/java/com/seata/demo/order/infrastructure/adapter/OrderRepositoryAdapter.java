package com.seata.demo.order.infrastructure.adapter;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.seata.demo.order.application.port.out.OrderRepositoryPort;
import com.seata.demo.order.domain.order.aggregate.root.Order;
import com.seata.demo.order.domain.order.aggregate.vo.Money;
import com.seata.demo.order.domain.order.aggregate.vo.OrderId;
import com.seata.demo.order.domain.order.aggregate.vo.OrderStatus;
import com.seata.demo.order.domain.order.aggregate.vo.ProductId;
import com.seata.demo.order.domain.order.aggregate.vo.Quantity;
import com.seata.demo.order.domain.order.aggregate.vo.UserId;
import com.seata.demo.order.infrastructure.persistence.order.entity.OrderJpaEntity;
import com.seata.demo.order.infrastructure.persistence.order.repository.OrderJpaRepository;

/**
 * 訂單儲存庫 Outbound 適配器 (Order Repository Adapter)
 * 命名規範 3-9: ... + Adapter
 */
@Component
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private final OrderJpaRepository orderJpaRepository;

    public OrderRepositoryAdapter(OrderJpaRepository orderJpaRepository) {
        this.orderJpaRepository = orderJpaRepository;
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return orderJpaRepository.findById(orderId.getValue())
                .map(this::toDomain);
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = toEntity(order);
        // 使用 saveAndFlush 強制即時刷新 JDBC，供 Seata AT 截取鏡像
        OrderJpaEntity saved = orderJpaRepository.saveAndFlush(entity);
        return toDomain(saved);
    }

    private Order toDomain(OrderJpaEntity entity) {
        return new Order(
                OrderId.of(entity.getId()),
                UserId.of(entity.getUserId()),
                ProductId.of(entity.getProductId()),
                Quantity.of(entity.getCount()),
                Money.of(entity.getAmount()),
                OrderStatus.valueOf(entity.getStatus())
        );
    }

    private OrderJpaEntity toEntity(Order domain) {
        return OrderJpaEntity.builder()
                .id(domain.getId().getValue())
                .userId(domain.getUserId().getValue())
                .productId(domain.getProductId().getValue())
                .count(domain.getCount().getValue())
                .amount(domain.getAmount().getValue())
                .status(domain.getStatus().name())
                .gmtCreate(LocalDateTime.now())
                .gmtModified(LocalDateTime.now())
                .build();
    }
}
