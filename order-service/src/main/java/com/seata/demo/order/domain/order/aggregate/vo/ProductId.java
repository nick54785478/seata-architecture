package com.seata.demo.order.domain.order.aggregate.vo;

import java.io.Serializable;
import java.util.Objects;

/**
 * 商品識別標識值物件 (Product ID Value Object)
 */
public final class ProductId implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long value;

    public ProductId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("ProductId 必須為大於 0 的正整數");
        }
        this.value = value;
    }

    public static ProductId of(Long value) {
        return new ProductId(value);
    }

    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductId productId = (ProductId) o;
        return Objects.equals(value, productId.value);
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
