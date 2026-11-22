package com.seata.demo.account.application.port.in;

import com.seata.demo.account.application.dto.command.PrepareAccountPaymentCommand;

/**
 * 預備帳戶支付 Inbound Port (Prepare Account Payment UseCase)
 * 命名規範 3-8: ... + UseCase
 * TCC 一階段業務合約介面。
 *
 * @author Seata Architecture Demo Team
 */
public interface PrepareAccountPaymentUseCase {

    /**
     * 執行 TCC 一階段：凍結資金並記錄防懸掛狀態
     *
     * @param command 預備支付命令
     * @return 凍結是否成功
     */
    boolean preparePayment(PrepareAccountPaymentCommand command);
}
