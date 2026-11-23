package com.seata.demo.order.application.port.out;

/**
 * 遠程庫存微服務呼叫 Outbound Port 介面 (Storage Client Port)
 * 命名規範 3-8: ... + Port
 *
 * @author Seata Architecture Demo Team
 */
public interface StorageClientPort {

    /**
     * 遠程調用庫存服務扣減庫存 (Seata AT 模式，透傳 XID)
     *
     * @param productId 商品 ID
     * @param count     扣減數量
     */
    void deductStorage(Long productId, Integer count);
}
