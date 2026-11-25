package com.seata.demo.order.presentation.resource.in;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 建立訂單請求載體 (Create Order Resource)
 * 命名規範 3-1: V + N + Resource
 * 表現層純資料載體。
 */
@Schema(description = "建立訂單請求載體")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderResource {

    @Schema(description = "用戶 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "用戶ID不可為空")
    @Positive(message = "用戶ID必須為大於0的正整數")
    private Long userId;

    @Schema(description = "商品 ID", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品ID不可為空")
    @Positive(message = "商品ID必須為大於0的正整數")
    private Long productId;

    @Schema(description = "購買數量", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "購買數量不可為空")
    @Positive(message = "購買數量必須大於0")
    private Integer count;

    @Schema(description = "支付總金額", example = "200.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "支付金額不可為空")
    @DecimalMin(value = "0.01", message = "支付金額必須大於0")
    private BigDecimal payAmount;

    @Schema(description = "是否故意模擬異常 (true 觸發分散式回滾)", example = "false")
    private Boolean simulateError;
}
