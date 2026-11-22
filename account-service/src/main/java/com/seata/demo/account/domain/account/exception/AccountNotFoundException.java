package com.seata.demo.account.domain.account.exception;

/**
 * 查無用戶帳戶領域例外 (Account Not Found Domain Exception)
 * 位於 Domain 層，當查詢不存在之用戶帳戶時拋出。
 *
 * @author Seata Architecture Demo Team
 */
public class AccountNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AccountNotFoundException(String message) {
        super(message);
    }

    public AccountNotFoundException(Long userId) {
        super(String.format("查無此用戶帳戶記錄, UserId: %d", userId));
    }
}
