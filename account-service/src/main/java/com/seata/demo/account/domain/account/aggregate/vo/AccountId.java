package com.seata.demo.account.domain.account.aggregate.vo;

import java.io.Serializable;
import java.util.Objects;

/**
 * 帳戶唯一識別標識值物件 (Account ID Value Object)
 * 純 Java 實作，無外層框架依賴。
 *
 * @author Seata Architecture Demo Team
 */
public final class AccountId implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long value;

    public AccountId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("AccountId 必須為大於 0 的正整數");
        }
        this.value = value;
    }

    public static AccountId of(Long value) {
        return new AccountId(value);
    }

    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountId accountId = (AccountId) o;
        return Objects.equals(value, accountId.value);
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
