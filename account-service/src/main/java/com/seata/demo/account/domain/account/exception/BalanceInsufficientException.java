package com.seata.demo.account.domain.account.exception;

import java.math.BigDecimal;

/**
 * 帳戶餘額不足領域例外 (Balance Insufficient Domain Exception)
 * 當扣減或凍結金額大於可用餘額時拋出。
 */
public class BalanceInsufficientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BalanceInsufficientException(String message) {
        super(message);
    }

    public BalanceInsufficientException(Long userId, BigDecimal requested, BigDecimal available) {
        super(String.format("用戶 [ID: %d] 可用餘額不足，請求凍結/扣減: %s, 目前可用餘額: %s", userId, requested, available));
    }
}
