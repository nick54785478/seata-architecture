package com.seata.demo.account.application.dto.command;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 預備帳戶支付命令 (Prepare Account Payment Command)
 * 命名規範 3-4: V + N + Command
 * TCC 一階段 Try 參數載體。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrepareAccountPaymentCommand {

    private String xid;
    private Long branchId;
    private Long userId;
    private BigDecimal amount;
}
