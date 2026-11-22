package com.seata.demo.storage.application.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 查詢商品庫存業務 Query (Get Storage Query)
 * 命名規範 3-5: V + N + Query
 * 位於 Application 層，純資料載體。
 *
 * @author Seata Architecture Demo Team
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetStorageQuery {

    /**
     * 商品 ID
     */
    private Long productId;
}
