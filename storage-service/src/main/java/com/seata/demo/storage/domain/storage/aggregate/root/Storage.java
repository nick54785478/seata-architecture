package com.seata.demo.storage.domain.storage.aggregate.root;

import com.seata.demo.storage.domain.storage.aggregate.vo.ProductId;
import com.seata.demo.storage.domain.storage.aggregate.vo.Quantity;
import com.seata.demo.storage.domain.storage.aggregate.vo.StorageId;
import com.seata.demo.storage.domain.storage.event.StockDeductedEvent;
import com.seata.demo.storage.domain.storage.exception.StockInsufficientException;

/**
 * 庫存聚合根 (Storage Aggregate Root)
 * 純 Java 實作，遵循 DDD 規範：嚴禁使用 Lombok 註解，屬性完全封裝，透過具備業務語意的行為方法進行異動。
 */
public class Storage {

    private final StorageId id;
    private final ProductId productId;
    private Quantity total;
    private Quantity used;
    private Quantity residue;

    /**
     * 重構或建立完整聚合根之建構子
     *
     * @param id        庫存 ID
     * @param productId 商品 ID
     * @param total     總庫存
     * @param used      已用庫存
     * @param residue   剩餘庫存
     */
    public Storage(StorageId id, ProductId productId, Quantity total, Quantity used, Quantity residue) {
        this.id = id;
        this.productId = productId;
        this.total = total;
        this.used = used;
        this.residue = residue;
    }

    /**
     * 核心業務行為：扣減庫存
     *
     * @param deductCount 扣減數量
     * @return 庫存扣減領域事件
     * @throws StockInsufficientException 當剩餘庫存小於扣減數量時拋出
     */
    public StockDeductedEvent deduct(Quantity deductCount) {
        if (!this.residue.isGreaterThanOrEqualTo(deductCount)) {
            throw new StockInsufficientException(this.productId.getValue(), deductCount.getValue(),
                    this.residue.getValue());
        }

        this.residue = this.residue.subtract(deductCount);
        this.used = this.used.add(deductCount);

        return new StockDeductedEvent(this.productId.getValue(), deductCount.getValue(), this.residue.getValue());
    }

    public StorageId getId() {
        return id;
    }

    public ProductId getProductId() {
        return productId;
    }

    public Quantity getTotal() {
        return total;
    }

    public Quantity getUsed() {
        return used;
    }

    public Quantity getResidue() {
        return residue;
    }
}
