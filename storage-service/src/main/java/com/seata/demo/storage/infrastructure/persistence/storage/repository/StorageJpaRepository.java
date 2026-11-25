package com.seata.demo.storage.infrastructure.persistence.storage.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.seata.demo.storage.infrastructure.persistence.storage.entity.StorageJpaEntity;

/**
 * 庫存 Spring Data JPA 資料庫存取介面 (Storage JPA Repository)
 */
@Repository
public interface StorageJpaRepository extends JpaRepository<StorageJpaEntity, Long> {

    /**
     * 依據商品 ID 查詢庫存記錄
     *
     * @param productId 商品 ID
     * @return JPA 實體封裝
     */
    Optional<StorageJpaEntity> findByProductId(Long productId);
}
