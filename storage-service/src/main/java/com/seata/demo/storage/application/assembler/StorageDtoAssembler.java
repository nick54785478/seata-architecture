package com.seata.demo.storage.application.assembler;

import com.seata.demo.storage.application.dto.result.StorageGottenResult;
import com.seata.demo.storage.domain.storage.aggregate.root.Storage;

/**
 * 庫存 DTO 轉換器 (Storage DTO Assembler)
 * 嚴守單一職責原則，負責領域模型與 DTO 的相互映射轉換。
 *
 * @author Seata Architecture Demo Team
 */
public final class StorageDtoAssembler {

    private StorageDtoAssembler() {}

    /**
     * 將 Storage 領域聚合根轉換為 StorageGottenResult DTO
     *
     * @param storage 領域聚合根
     * @return DTO 資料載體
     */
    public static StorageGottenResult toResult(Storage storage) {
        if (storage == null) {
            return null;
        }
        return StorageGottenResult.builder()
                .id(storage.getId().getValue())
                .productId(storage.getProductId().getValue())
                .total(storage.getTotal().getValue())
                .used(storage.getUsed().getValue())
                .residue(storage.getResidue().getValue())
                .build();
    }
}
