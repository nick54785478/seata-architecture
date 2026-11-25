package com.seata.demo.storage.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 庫存查詢結果 DTO (Storage Gotten Result DTO)
 * 命名規範 3-6: N + Gotten/Searched + Result
 * 純資料載體，禁止包含任何 fromDomain 等轉換邏輯。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageGottenResult {

    private Long id;
    private Long productId;
    private Integer total;
    private Integer used;
    private Integer residue;
}
