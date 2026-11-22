package com.seata.demo.account.application.dto.command;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 確認帳戶支付命令 (Confirm Account Payment Command)
 * 命名規範 3-4: V + N + Command
 * TCC 二階段 Confirm 參數載體。
 *
 * @author Seata Architecture Demo Team
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmAccountPaymentCommand {

    private String xid;
    private Long branchId;
    private Long userId;
    private BigDecimal amount;
}
