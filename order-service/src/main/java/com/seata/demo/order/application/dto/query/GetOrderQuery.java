package com.seata.demo.order.application.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 查詢訂單 Query (Get Order Query)
 * 命名規範 3-5: V + N + Query
 * 純資料載體。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetOrderQuery {

    /**
     * 訂單識別碼 (Order ID)
     */
    private Long orderId;
}
