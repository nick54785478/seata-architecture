package com.seata.demo.account.infrastructure.persistence.account.entity;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 帳戶資產 JPA ORM 實體 (Account JPA Entity)
 * 位於 Infrastructure 層，專門映射 MySQL 表結構。
 *
 * @author Seata Architecture Demo Team
 */
@Entity
@Table(name = "t_account")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountJpaEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance;

    @Column(name = "freeze_amount", nullable = false)
    private BigDecimal freezeAmount;
}
