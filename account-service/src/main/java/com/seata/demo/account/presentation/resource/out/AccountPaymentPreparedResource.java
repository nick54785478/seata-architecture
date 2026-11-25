package com.seata.demo.account.presentation.resource.out;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 帳戶支付已預備回應載體 (Account Payment Prepared Resource)
 * 命名規範 3-2: N + Ved + Resource
 * 表現層純資料載體。
 */
@Schema(description = "帳戶支付已預備回應載體")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountPaymentPreparedResource {

    @Schema(description = "用戶 ID", example = "1")
    private Long userId;

    @Schema(description = "本次凍結金額", example = "200.00")
    private BigDecimal frozenAmount;

    @Schema(description = "處理狀態", example = "PREPARED")
    private String status;
}
