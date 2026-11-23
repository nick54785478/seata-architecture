package com.seata.demo.storage.presentation.resource.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 庫存詳情查詢回應載體 (Storage Retrieved Resource)
 * 命名規範 3-2: N + Ved + Resource
 * 位於 Presentation 層，純資料載體。
 *
 * @author Seata Architecture Demo Team
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商品庫存查詢回應資料")
public class StorageRetrievedResource {

    @Schema(description = "商品 ID", example = "20001")
    private Long productId;

    @Schema(description = "商品總庫存", example = "100")
    private Integer total;

    @Schema(description = "已扣減庫存數量", example = "2")
    private Integer used;

    @Schema(description = "即時剩餘可用庫存", example = "98")
    private Integer residue;
}
