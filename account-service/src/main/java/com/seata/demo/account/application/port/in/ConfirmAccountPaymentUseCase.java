package com.seata.demo.account.application.port.in;

import com.seata.demo.account.application.dto.command.ConfirmAccountPaymentCommand;

/**
 * 確認帳戶支付 Inbound Port (Confirm Account Payment UseCase)
 * 命名規範 3-8: ... + UseCase
 * TCC 二階段 Confirm 業務合約介面。
 *
 * @author Seata Architecture Demo Team
 */
public interface ConfirmAccountPaymentUseCase {

    /**
     * 執行 TCC 二階段 Confirm：正式劃轉扣減凍結資金
     *
     * @param command 確認支付命令
     * @return 確認是否成功
     */
    boolean confirmPayment(ConfirmAccountPaymentCommand command);
}
