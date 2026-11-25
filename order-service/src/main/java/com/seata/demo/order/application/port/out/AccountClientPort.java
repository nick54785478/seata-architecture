package com.seata.demo.order.application.port.out;

import java.math.BigDecimal;

/**
 * 遠程帳戶微服務呼叫 Outbound Port 介面 (Account Client Port)
 * 命名規範 3-8: ... + Port
 */
public interface AccountClientPort {

    /**
     * 遠程調用帳戶服務凍結金額 (Seata TCC 一階段 Try，透傳 XID)
     *
     * @param userId 用戶 ID
     * @param amount 支付金額
     */
    void preparePayment(Long userId, BigDecimal amount);
}
