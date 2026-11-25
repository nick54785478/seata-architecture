package com.seata.demo.storage.domain.storage.event;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 庫存已扣減領域事件 (Stock Deducted Domain Event)
 * 符合命名規範 3-3: N + Ved + Event
 */
public final class StockDeductedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long productId;
    private final Integer deductedQuantity;
    private final Integer remainingQuantity;
    private final LocalDateTime occurredOn;

    public StockDeductedEvent(Long productId, Integer deductedQuantity, Integer remainingQuantity) {
        this.productId = productId;
        this.deductedQuantity = deductedQuantity;
        this.remainingQuantity = remainingQuantity;
        this.occurredOn = LocalDateTime.now();
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getDeductedQuantity() {
        return deductedQuantity;
    }

    public Integer getRemainingQuantity() {
        return remainingQuantity;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}
