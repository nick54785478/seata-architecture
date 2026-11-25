package com.seata.demo.order.infrastructure.persistence.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.seata.demo.order.infrastructure.persistence.order.entity.OrderJpaEntity;

/**
 * 訂單 Spring Data JPA 資料庫存取介面
 **/
@Repository
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {
}
