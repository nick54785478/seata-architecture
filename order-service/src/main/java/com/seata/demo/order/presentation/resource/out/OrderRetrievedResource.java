package com.seata.demo.order.presentation.resource.out;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 訂單已查詢回應載體 (Order Retrieved Resource)
 * 命名規範 3-2: N + Ved + Resource
 * 表現層純資料載體。
 *
 * @author Seata Architecture Demo Team
 */
@Schema(description = "訂單已查詢回應載體")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRetrievedResource {

    @Schema(description = "訂單 ID", example = "1001")
    private Long orderId;

    @Schema(description = "用戶 ID", example = "10001")
    private Long userId;

    @Schema(description = "商品 ID", example = "20001")
    private Long productId;

    @Schema(description = "購買數量", example = "2")
    private Integer count;

    @Schema(description = "訂單金額", example = "500.00")
    private BigDecimal amount;

    @Schema(description = "訂單狀態", example = "SUCCESS")
    private String status;
}
