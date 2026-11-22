package com.seata.demo.storage.domain.storage.aggregate.vo;

import java.io.Serializable;
import java.util.Objects;

/**
 * 庫存數量值物件 (Quantity Value Object)
 * 純 Java 實作，無外層框架依賴。
 *
 * @author Seata Architecture Demo Team
 */
public final class Quantity implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Integer value;

    public Quantity(Integer value) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("Quantity 必須為大於或等於 0 的整數");
        }
        this.value = value;
    }

    public static Quantity of(Integer value) {
        return new Quantity(value);
    }

    public static Quantity zero() {
        return new Quantity(0);
    }

    public Integer getValue() {
        return value;
    }

    public Quantity add(Quantity other) {
        return new Quantity(this.value + other.value);
    }

    public Quantity subtract(Quantity other) {
        if (this.value < other.value) {
            throw new IllegalArgumentException("庫存不足扣減，無法為負數");
        }
        return new Quantity(this.value - other.value);
    }

    public boolean isGreaterThanOrEqualTo(Quantity other) {
        return this.value >= other.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quantity quantity = (Quantity) o;
        return Objects.equals(value, quantity.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
