package com.seata.demo.storage.infrastructure.persistence.storage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 庫存 JPA ORM 實體 (Storage JPA Entity)
 * 位於 Infrastructure 層，專注於關聯式資料庫映射，嚴禁侵入 Domain 層。
 *
 * @author Seata Architecture Demo Team
 */
@Entity
@Table(name = "t_storage")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "total", nullable = false)
    private Integer total;

    @Column(name = "used", nullable = false)
    private Integer used;

    @Column(name = "residue", nullable = false)
    private Integer residue;
}
