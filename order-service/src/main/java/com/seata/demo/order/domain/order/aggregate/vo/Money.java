package com.seata.demo.order.domain.order.aggregate.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 訂單金額值物件 (Money Value Object)
 *
 * @author Seata Architecture Demo Team
 */
public final class Money implements Serializable {

    private static final long serialVersionUID = 1L;

    private final BigDecimal value;

    public Money(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金額必須大於或等於 0");
        }
        this.value = value.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    public BigDecimal getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return Objects.equals(value, money.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
