package com.seata.demo.order.domain.order.aggregate.vo;

import java.io.Serializable;
import java.util.Objects;

/**
 * 訂單唯一識別標識值物件 (Order ID Value Object)
 * 純 Java 實作，無外層框架依賴。
 *
 * @author Seata Architecture Demo Team
 */
public final class OrderId implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long value;

    public OrderId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("OrderId 必須為大於 0 的正整數");
        }
        this.value = value;
    }

    public static OrderId of(Long value) {
        return new OrderId(value);
    }

    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderId orderId = (OrderId) o;
        return Objects.equals(value, orderId.value);
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
