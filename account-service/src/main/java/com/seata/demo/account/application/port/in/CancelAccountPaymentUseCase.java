package com.seata.demo.account.application.port.in;

import com.seata.demo.account.application.dto.command.CancelAccountPaymentCommand;

/**
 * 取消帳戶支付 Inbound Port (Cancel Account Payment UseCase)
 * 命名規範 3-8: ... + UseCase
 * TCC 二階段 Cancel 業務合約介面。
 *
 * @author Seata Architecture Demo Team
 */
public interface CancelAccountPaymentUseCase {

    /**
     * 執行 TCC 二階段 Cancel：解凍資金並退回可用餘額 (防空回滾 & 冪等保障)
     *
     * @param command 取消支付命令
     * @return 取消是否成功
     */
    boolean cancelPayment(CancelAccountPaymentCommand command);
}
