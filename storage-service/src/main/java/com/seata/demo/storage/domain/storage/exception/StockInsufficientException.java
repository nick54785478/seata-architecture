package com.seata.demo.storage.domain.storage.exception;

/**
 * 庫存不足領域例外 (Stock Insufficient Domain Exception)
 * 當扣減量大於當前剩餘庫存時拋出。
 */
public class StockInsufficientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StockInsufficientException(String message) {
        super(message);
    }

    public StockInsufficientException(Long productId, Integer requested, Integer available) {
        super(String.format("商品 [ID: %d] 可用庫存不足，請求扣減: %d, 目前剩餘: %d", productId, requested, available));
    }
}
