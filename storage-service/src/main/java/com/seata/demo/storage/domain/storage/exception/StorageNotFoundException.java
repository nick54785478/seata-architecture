package com.seata.demo.storage.domain.storage.exception;

/**
 * 查無商品庫存領域例外 (Storage Not Found Domain Exception)
 * 位於 Domain 層，當查詢不存在之商品庫存時拋出。
 *
 * @author Seata Architecture Demo Team
 */
public class StorageNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StorageNotFoundException(String message) {
        super(message);
    }

    public StorageNotFoundException(Long productId) {
        super(String.format("查無此商品庫存記錄, ProductId: %d", productId));
    }
}
