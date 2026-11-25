package com.seata.demo.storage.presentation.resource.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 庫存已扣減回應載體 (Storage Deducted Resource)
 * 命名規範 3-2: N + Ved + Resource
 * 表現層純資料載體，嚴禁在此實作 fromResult 等轉換邏輯。
 */
@Schema(description = "庫存已扣減回應載體")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageDeductedResource {

    @Schema(description = "商品 ID", example = "100")
    private Long productId;

    @Schema(description = "已扣減數量", example = "2")
    private Integer deductedCount;

    @Schema(description = "處理狀態", example = "DEDUCTED")
    private String status;
}
