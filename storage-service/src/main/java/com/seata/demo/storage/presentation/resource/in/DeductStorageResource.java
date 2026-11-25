package com.seata.demo.storage.presentation.resource.in;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 扣減庫存請求載體 (Deduct Storage Resource)
 * 命名規範 3-1: V + N + Resource
 * 表現層純資料載體，嚴禁在此實作 toCommand 等轉換邏輯。
 */
@Schema(description = "扣減庫存請求載體")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeductStorageResource {

    @Schema(description = "商品 ID", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品ID不可為空")
    @Positive(message = "商品ID必須為大於0的正整數")
    private Long productId;

    @Schema(description = "扣減數量", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "扣減數量不可為空")
    @Positive(message = "扣減數量必須大於0")
    private Integer count;
}
