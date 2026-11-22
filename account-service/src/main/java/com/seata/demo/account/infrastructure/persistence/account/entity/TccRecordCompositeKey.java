package com.seata.demo.account.infrastructure.persistence.account.entity;

import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * TCC 記錄複合主鍵類 (TCC Record Composite Key)
 *
 * @author Seata Architecture Demo Team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TccRecordCompositeKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private String xid;
    private Long branchId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TccRecordCompositeKey that = (TccRecordCompositeKey) o;
        return Objects.equals(xid, that.xid) && Objects.equals(branchId, that.branchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xid, branchId);
    }
}
