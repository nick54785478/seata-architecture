package com.seata.demo.order.domain.order.aggregate.vo;

import java.io.Serializable;
import java.util.Objects;

/**
 * 數量值物件 (Quantity Value Object)
 *
 * @author Seata Architecture Demo Team
 */
public final class Quantity implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Integer value;

    public Quantity(Integer value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Quantity 必須大於 0");
        }
        this.value = value;
    }

    public static Quantity of(Integer value) {
        return new Quantity(value);
    }

    public Integer getValue() {
        return value;
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
