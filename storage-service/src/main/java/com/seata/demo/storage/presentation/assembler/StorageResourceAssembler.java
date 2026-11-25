package com.seata.demo.storage.presentation.assembler;

import com.seata.demo.storage.application.dto.command.DeductStorageCommand;
import com.seata.demo.storage.presentation.resource.in.DeductStorageResource;
import com.seata.demo.storage.presentation.resource.out.StorageDeductedResource;
import com.seata.demo.storage.presentation.resource.out.StorageRetrievedResource;

/**
 * 表現層資源轉換器 (Storage Resource Assembler)
 * 負責 Resource 與 Command/Result 之間的轉換 (Anti-Corruption 防腐層職責)。
 */
public final class StorageResourceAssembler {

    private StorageResourceAssembler() {
    }

    /**
     * 將 Request Resource 轉換為應用層 Command
     *
     * @param resource 請求載體
     * @return 扣減命令
     */
    public static DeductStorageCommand toCommand(DeductStorageResource resource) {
        if (resource == null) {
            return null;
        }
        return DeductStorageCommand.builder()
                .productId(resource.getProductId())
                .count(resource.getCount())
                .build();
    }

    /**
     * 建構回應載體
     *
     * @param productId 商品 ID
     * @param count     扣減數量
     * @return 回應載體
     */
    public static StorageDeductedResource toResource(Long productId, Integer count) {
        return StorageDeductedResource.builder()
                .productId(productId)
                .deductedCount(count)
                .status("SUCCESS")
                .build();
    }

    /**
     * 將應用層 DTO 轉換為查詢回應載體
     *
     * @param result 庫存查詢結果 DTO
     * @return 庫存查詢回應載體
     */
    public static StorageRetrievedResource toRetrievedResource(
            com.seata.demo.storage.application.dto.result.StorageGottenResult result) {
        if (result == null) {
            return null;
        }
        return StorageRetrievedResource.builder()
                .productId(result.getProductId())
                .total(result.getTotal())
                .used(result.getUsed())
                .residue(result.getResidue())
                .build();
    }
}
