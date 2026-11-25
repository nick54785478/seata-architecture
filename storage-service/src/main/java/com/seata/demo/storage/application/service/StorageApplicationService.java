package com.seata.demo.storage.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seata.demo.storage.application.assembler.StorageDtoAssembler;
import com.seata.demo.storage.application.dto.command.DeductStorageCommand;
import com.seata.demo.storage.application.dto.query.GetStorageQuery;
import com.seata.demo.storage.application.dto.result.StorageGottenResult;
import com.seata.demo.storage.application.port.in.DeductStorageUseCase;
import com.seata.demo.storage.application.port.in.GetStorageUseCase;
import com.seata.demo.storage.application.port.out.StorageRepositoryPort;
import com.seata.demo.storage.domain.storage.aggregate.root.Storage;
import com.seata.demo.storage.domain.storage.aggregate.vo.ProductId;
import com.seata.demo.storage.domain.storage.aggregate.vo.Quantity;
import com.seata.demo.storage.domain.storage.event.StockDeductedEvent;
import com.seata.demo.storage.domain.storage.exception.StockInsufficientException;
import com.seata.demo.storage.domain.storage.exception.StorageNotFoundException;

/**
 * 庫存應用層服務 (Storage Application Service)
 * 命名規範 3-9: ... + ApplicationService
 * 實作 DeductStorageUseCase 與 GetStorageUseCase，負責協調領域聚合根與 Outbound Port。
 * 參與 Seata AT 模式分支事務，在一階段本地事務中提交，由 DataSourceProxy 記錄 undo_log。
 */
@Service
public class StorageApplicationService implements DeductStorageUseCase, GetStorageUseCase {

    private static final Logger log = LoggerFactory.getLogger(StorageApplicationService.class);

    private final StorageRepositoryPort storageRepositoryPort;

    public StorageApplicationService(StorageRepositoryPort storageRepositoryPort) {
        this.storageRepositoryPort = storageRepositoryPort;
    }

    /**
     * 依據商品 ID 查詢即時庫存詳情
     *
     * @param query 查詢條件物件
     * @return 庫存查詢結果 DTO
     */
    @Override
    @Transactional(readOnly = true)
    public StorageGottenResult getStorage(GetStorageQuery query) {
        log.info("[StorageApplicationService] 查詢商品庫存, ProductId = {}", query.getProductId());
        Storage storage = storageRepositoryPort.findByProductId(ProductId.of(query.getProductId()))
                .orElseThrow(() -> new StorageNotFoundException(query.getProductId()));
        return StorageDtoAssembler.toResult(storage);
    }

    /**
     * 執行庫存扣減業務流程
     *
     * @param command 扣減庫存命令
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductStorage(DeductStorageCommand command) {
        log.info("[StorageApplicationService] 開始執行庫存扣減, ProductId = {}, Count = {}",
                command.getProductId(), command.getCount());

        ProductId productId = ProductId.of(command.getProductId());
        Quantity deductQuantity = Quantity.of(command.getCount());

        // 1. 透過 Outbound Port 獲取領域聚合根
        Storage storage = storageRepositoryPort.findByProductId(productId)
                .orElseThrow(() -> new StockInsufficientException("查無該商品之庫存記錄, ProductId: " + command.getProductId()));

        // 2. 驅動聚合根執行核心業務規則
        StockDeductedEvent event = storage.deduct(deductQuantity);

        // 3. 透過 Port 保存聚合根異動 (JPA 即時 flush)
        storageRepositoryPort.save(storage);

        log.info("[StorageApplicationService] 庫存扣減成功, ProductId = {}, 扣減量 = {}, 剩餘量 = {}",
                event.getProductId(), event.getDeductedQuantity(), event.getRemainingQuantity());
    }
}
