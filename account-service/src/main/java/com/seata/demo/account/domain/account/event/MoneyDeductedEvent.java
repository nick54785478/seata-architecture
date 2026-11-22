package com.seata.demo.account.domain.account.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 資金已扣除領域事件 (Money Deducted Domain Event)
 * 符合命名規範 3-3: N + Ved + Event
 *
 * @author Seata Architecture Demo Team
 */
public final class MoneyDeductedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final BigDecimal deductedAmount;
    private final LocalDateTime occurredOn;

    public MoneyDeductedEvent(Long userId, BigDecimal deductedAmount) {
        this.userId = userId;
        this.deductedAmount = deductedAmount;
        this.occurredOn = LocalDateTime.now();
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getDeductedAmount() {
        return deductedAmount;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}
