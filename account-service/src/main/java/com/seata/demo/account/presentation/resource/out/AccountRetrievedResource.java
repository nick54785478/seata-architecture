package com.seata.demo.account.presentation.resource.out;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 帳戶詳情查詢回應載體 (Account Retrieved Resource)
 * 命名規範 3-2: N + Ved + Resource
 * 位於 Presentation 層，純資料載體。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用戶帳戶資產查詢回應資料")
public class AccountRetrievedResource {

    @Schema(description = "用戶 ID", example = "10001")
    private Long userId;

    @Schema(description = "當前可用餘額", example = "9500.00")
    private BigDecimal balance;

    @Schema(description = "TCC 一階段凍結金額", example = "0.00")
    private BigDecimal freezeAmount;

    @Schema(description = "總資產 (可用餘額 + 凍結金額)", example = "9500.00")
    private BigDecimal totalAmount;
}
