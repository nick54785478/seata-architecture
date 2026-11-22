package com.seata.demo.order.domain.order.aggregate.vo;

import java.io.Serializable;
import java.util.Objects;

/**
 * 用戶識別標識值物件 (User ID Value Object)
 *
 * @author Seata Architecture Demo Team
 */
public final class UserId implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long value;

    public UserId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("UserId 必須為大於 0 的正整數");
        }
        this.value = value;
    }

    public static UserId of(Long value) {
        return new UserId(value);
    }

    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserId userId = (UserId) o;
        return Objects.equals(value, userId.value);
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
