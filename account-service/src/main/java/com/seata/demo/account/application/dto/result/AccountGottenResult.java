package com.seata.demo.account.application.dto.result;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 帳戶查詢結果 DTO (Account Gotten Result)
 * 命名規範 3-6: N + Gotten/Searched + Result
 * 純資料載體，不含業務邏輯。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountGottenResult {

    private Long userId;
    private BigDecimal balance;
    private BigDecimal freezeAmount;
}
