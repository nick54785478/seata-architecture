package com.seata.demo.account.domain.account.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 資金已解凍領域事件 (Money Unfrozen Domain Event)
 * 符合命名規範 3-3: N + Ved + Event
 *
 * @author Seata Architecture Demo Team
 */
public final class MoneyUnfrozenEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final BigDecimal unfrozenAmount;
    private final BigDecimal currentBalance;
    private final LocalDateTime occurredOn;

    public MoneyUnfrozenEvent(Long userId, BigDecimal unfrozenAmount, BigDecimal currentBalance) {
        this.userId = userId;
        this.unfrozenAmount = unfrozenAmount;
        this.currentBalance = currentBalance;
        this.occurredOn = LocalDateTime.now();
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getUnfrozenAmount() {
        return unfrozenAmount;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}
