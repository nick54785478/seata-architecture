package com.seata.demo.account.infrastructure.persistence.account.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.seata.demo.account.infrastructure.persistence.account.entity.AccountJpaEntity;

/**
 * 帳戶 Spring Data JPA 資料庫存取介面
 */
@Repository
public interface AccountJpaRepository extends JpaRepository<AccountJpaEntity, Long> {
}
