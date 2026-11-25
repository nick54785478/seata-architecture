package com.seata.demo.storage.domain.storage.aggregate.vo;

import java.io.Serializable;
import java.util.Objects;

/**
 * 庫存實體唯一識別標識值物件 (Storage ID Value Object)
 * 純 Java 實作，無外層框架依賴。
 */
public final class StorageId implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long value;

    public StorageId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("StorageId 必須為大於 0 的正整數");
        }
        this.value = value;
    }

    public static StorageId of(Long value) {
        return new StorageId(value);
    }

    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        StorageId storageId = (StorageId) o;
        return Objects.equals(value, storageId.value);
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
