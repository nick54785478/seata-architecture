package com.seata.demo.order.application.dto.result;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 訂單查詢結果 DTO (Order Gotten Result)
 * 命名規範 3-6: N + Gotten/Searched + Result
 * 純資料載體。
 *
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderGottenResult {

    private Long orderId;
    private Long userId;
    private Long productId;
    private Integer count;
    private BigDecimal amount;
    private String status;
}
