package com.seata.demo.storage.application.port.out;

import java.util.Optional;
import com.seata.demo.storage.domain.storage.aggregate.root.Storage;
import com.seata.demo.storage.domain.storage.aggregate.vo.ProductId;

/**
 * 庫存儲存庫 Outbound Port 介面 (Storage Repository Port)
 * 命名規範 3-8: ... + Port
 * 定義應用層對持久化基礎設施的要求合約。
 * 嚴禁傳入或回傳任何 ORM Entity 或 JDBC 技術類物件。
 */
public interface StorageRepositoryPort {

    /**
     * 依據商品 ID 查詢庫存聚合根
     *
     * @param productId 商品 ID 值物件
     * @return 庫存領域聚合根 (Optional)
     */
    Optional<Storage> findByProductId(ProductId productId);

    /**
     * 保存庫存聚合根異動 (含一級快取即時刷新 flush)
     *
     * @param storage 庫存聚合根
     * @return 保存後之領域聚合根
     */
    Storage save(Storage storage);
}
