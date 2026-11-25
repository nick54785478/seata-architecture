package com.seata.demo.account.domain.account.aggregate.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 金額值物件 (Money Value Object)
 * 純 Java 實作，精確到小數點後兩位，無外層框架依賴。
 */
public final class Money implements Serializable {

    private static final long serialVersionUID = 1L;

    private final BigDecimal value;

    public Money(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Money 必須大於或等於 0");
        }
        this.value = value.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    public static Money of(double value) {
        return new Money(BigDecimal.valueOf(value));
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public BigDecimal getValue() {
        return value;
    }

    public Money add(Money other) {
        return new Money(this.value.add(other.value));
    }

    public Money subtract(Money other) {
        if (this.value.compareTo(other.value) < 0) {
            throw new IllegalArgumentException("金額不足扣減，無法為負數");
        }
        return new Money(this.value.subtract(other.value));
    }

    public boolean isGreaterThanOrEqualTo(Money other) {
        return this.value.compareTo(other.value) >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
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
