package com.seata.demo.order.infrastructure.persistence.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
 * 訂單 JPA ORM 實體 (Order JPA Entity)
 * 位於 Infrastructure 層。
 *
 * @author Seata Architecture Demo Team
 */
@Entity
@Table(name = "t_order")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderJpaEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "count", nullable = false)
    private Integer count;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "gmt_create")
    private LocalDateTime gmtCreate;

    @Column(name = "gmt_modified")
    private LocalDateTime gmtModified;
}
