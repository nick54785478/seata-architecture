package com.seata.demo.account.infrastructure.persistence.account.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * TCC 控制表記錄實體 (TCC Record JPA Entity)
 *
 * @author Seata Architecture Demo Team
 */
@Entity
@Table(name = "t_tcc_transaction_record")
@IdClass(TccRecordCompositeKey.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TccRecordJpaEntity {

    @Id
    @Column(name = "xid", length = 128)
    private String xid;

    @Id
    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "freeze_amount", nullable = false)
    private BigDecimal freezeAmount;

    @Column(name = "gmt_create", nullable = false)
    private LocalDateTime gmtCreate;
}
