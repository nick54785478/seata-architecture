package com.seata.demo.account.domain.account.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 資金已凍結領域事件 (Money Frozen Domain Event)
 * 符合命名規範 3-3: N + Ved + Event
 *
 * @author Seata Architecture Demo Team
 */
public final class MoneyFrozenEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final BigDecimal frozenAmount;
    private final BigDecimal remainingBalance;
    private final LocalDateTime occurredOn;

    public MoneyFrozenEvent(Long userId, BigDecimal frozenAmount, BigDecimal remainingBalance) {
        this.userId = userId;
        this.frozenAmount = frozenAmount;
        this.remainingBalance = remainingBalance;
        this.occurredOn = LocalDateTime.now();
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getFrozenAmount() {
        return frozenAmount;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}
