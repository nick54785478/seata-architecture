package com.seata.demo.account.infrastructure.persistence.account.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.seata.demo.account.infrastructure.persistence.account.entity.TccRecordCompositeKey;
import com.seata.demo.account.infrastructure.persistence.account.entity.TccRecordJpaEntity;

/**
 * TCC 控制表記錄 JPA 介面
 */
@Repository
public interface TccRecordJpaRepository extends JpaRepository<TccRecordJpaEntity, TccRecordCompositeKey> {

    Optional<TccRecordJpaEntity> findByXidAndBranchId(String xid, Long branchId);
}
