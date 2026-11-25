package com.seata.demo.account.application.dto.command;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 取消帳戶支付命令 (Cancel Account Payment Command)
 * 命名規範 3-4: V + N + Command
 * TCC 二階段 Cancel 參數載體。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelAccountPaymentCommand {

    private String xid;
    private Long branchId;
    private Long userId;
    private BigDecimal amount;
}
