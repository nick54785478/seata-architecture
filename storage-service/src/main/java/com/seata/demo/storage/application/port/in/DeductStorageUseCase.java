package com.seata.demo.storage.application.port.in;

import com.seata.demo.storage.application.dto.command.DeductStorageCommand;

/**
 * 庫存扣減案例 Inbound Port 介面 (Deduct Storage UseCase)
 * 命名規範 3-8: ... + UseCase
 * 代表應用層對外開放的業務合約，接收 Command 並執行領域邏輯。
 *
 * @author Seata Architecture Demo Team
 */
public interface DeductStorageUseCase {

    /**
     * 執行扣減庫存操作
     *
     * @param command 扣減庫存命令
     */
    void deductStorage(DeductStorageCommand command);
}
