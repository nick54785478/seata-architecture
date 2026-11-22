package com.seata.demo.order.domain.order.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 訂單已建立領域事件 (Order Created Domain Event)
 * 符合命名規範 3-3: N + Ved + Event
 *
 * @author Seata Architecture Demo Team
 */
public final class OrderCreatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long orderId;
    private final Long userId;
    private final Long productId;
    private final Integer count;
    private final BigDecimal amount;
    private final LocalDateTime occurredOn;

    public OrderCreatedEvent(Long orderId, Long userId, Long productId, Integer count, BigDecimal amount) {
        this.orderId = orderId;
        this.userId = userId;
        this.productId = productId;
        this.count = count;
        this.amount = amount;
        this.occurredOn = LocalDateTime.now();
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getCount() {
        return count;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}
