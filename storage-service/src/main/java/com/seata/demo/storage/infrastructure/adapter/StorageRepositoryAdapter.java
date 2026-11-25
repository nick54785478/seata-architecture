package com.seata.demo.storage.infrastructure.adapter;

import java.util.Optional;
import org.springframework.stereotype.Component;

import com.seata.demo.storage.application.port.out.StorageRepositoryPort;
import com.seata.demo.storage.domain.storage.aggregate.root.Storage;
import com.seata.demo.storage.domain.storage.aggregate.vo.ProductId;
import com.seata.demo.storage.domain.storage.aggregate.vo.Quantity;
import com.seata.demo.storage.domain.storage.aggregate.vo.StorageId;
import com.seata.demo.storage.infrastructure.persistence.storage.entity.StorageJpaEntity;
import com.seata.demo.storage.infrastructure.persistence.storage.repository.StorageJpaRepository;

/**
 * 庫存儲存庫 Outbound 適配器 (Storage Repository Adapter)
 * 命名規範 3-9: ... + Adapter
 * 實作應用層 StorageRepositoryPort 介面，協調 Spring Data JPA 進行持久化。
 * 執行 saveAndFlush 確保 Hibernate 即時送出 SQL 供 Seata DataSourceProxy 截獲生成 undo_log。
 */
@Component
public class StorageRepositoryAdapter implements StorageRepositoryPort {

    private final StorageJpaRepository storageJpaRepository;

    public StorageRepositoryAdapter(StorageJpaRepository storageJpaRepository) {
        this.storageJpaRepository = storageJpaRepository;
    }

    @Override
    public Optional<Storage> findByProductId(ProductId productId) {
        return storageJpaRepository.findByProductId(productId.getValue())
                .map(this::toDomain);
    }

    @Override
    public Storage save(Storage storage) {
        StorageJpaEntity entity = toEntity(storage);
        // 核心關鍵點：使用 saveAndFlush 強制即時刷新 JDBC，供 Seata AT 截取鏡像
        StorageJpaEntity savedEntity = storageJpaRepository.saveAndFlush(entity);
        return toDomain(savedEntity);
    }

    private Storage toDomain(StorageJpaEntity entity) {
        return new Storage(
                StorageId.of(entity.getId()),
                ProductId.of(entity.getProductId()),
                Quantity.of(entity.getTotal()),
                Quantity.of(entity.getUsed()),
                Quantity.of(entity.getResidue()));
    }

    private StorageJpaEntity toEntity(Storage domain) {
        return StorageJpaEntity.builder()
                .id(domain.getId() != null ? domain.getId().getValue() : null)
                .productId(domain.getProductId().getValue())
                .total(domain.getTotal().getValue())
                .used(domain.getUsed().getValue())
                .residue(domain.getResidue().getValue())
                .build();
    }
}
