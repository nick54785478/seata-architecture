package com.seata.demo.account.presentation.resource.in;

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
 * 預備帳戶支付請求載體 (Prepare Account Payment Resource)
 * 命名規範 3-1: V + N + Resource
 * 表現層純資料載體。
 */
@Schema(description = "預備帳戶支付請求載體 (TCC Try 階段)")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrepareAccountPaymentResource {

    @Schema(description = "用戶 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "用戶ID不可為空")
    @Positive(message = "用戶ID必須為大於0的正整數")
    private Long userId;

    @Schema(description = "預留/扣減金額", example = "200.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "支付金額不可為空")
    @DecimalMin(value = "0.01", message = "支付金額必須大於0")
    private BigDecimal amount;
}
