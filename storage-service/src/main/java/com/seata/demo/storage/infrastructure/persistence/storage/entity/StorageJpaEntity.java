package com.seata.demo.storage.infrastructure.persistence.storage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 庫存 JPA ORM 充血模型實體 (Storage JPA Rich Entity)
 * 位於 Infrastructure 層持久化模組，封裝關聯式資料庫欄位映射與核心庫存變更業務邏輯。
 * <p>
 * <b>【充血模型設計特性 (Rich Domain Model)】</b>
 * <ul>
 *   <li><b>移除公開無差別 Setter</b>：防禦外部任意篡改內部狀態，捍衛資料庫資料與業務邏輯一致性。</li>
 *   <li><b>封裝庫存守恆不變性 (Invariants)</b>：保證 {@code total == used + residue}。</li>
 *   <li><b>內聚業務行為方法</b>：提供 {@link #deduct(Integer)}、{@link #isAvailable(Integer)} 與 {@link #reset(Integer)} 等業務方法。</li>
 * </ul>
 */
@Entity
@Table(name = "t_storage")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class StorageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "total", nullable = false)
    private Integer total;

    @Column(name = "used", nullable = false)
    private Integer used;

    @Column(name = "residue", nullable = false)
    private Integer residue;

    /**
     * 靜態工廠方法：建立新商品庫存記錄
     *
     * @param productId 商品 ID
     * @param total     初始總庫存
     * @return 庫存充血實體 (初始 used 為 0，residue 為 total)
     */
    public static StorageJpaEntity create(Long productId, Integer total) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("商品 ID 必須為大於 0 的正整數");
        }
        if (total == null || total < 0) {
            throw new IllegalArgumentException("初始庫存不可為小於 0 的負數");
        }
        return StorageJpaEntity.builder()
                .productId(productId)
                .total(total)
                .used(0)
                .residue(total)
                .build();
    }

    /**
     * 業務行為：扣減庫存
     * 驗證剩餘庫存充足性，更新已用庫存與剩餘庫存，維護總量守恆不變性
     *
     * @param count 扣減數量
     * @throws IllegalArgumentException 當扣減數量非法時拋出
     * @throws IllegalStateException    當剩餘庫存不足時拋出
     */
    public void deduct(Integer count) {
        if (count == null || count <= 0) {
            throw new IllegalArgumentException("扣減數量必須為大於 0 的正整數");
        }
        if (!isAvailable(count)) {
            throw new IllegalStateException(String.format(
                    "商品 ID: %d 庫存不足！當前可用: %d, 欲扣減: %d", productId, residue, count));
        }
        this.residue -= count;
        this.used += count;
    }

    /**
     * 業務查詢：檢查指定數量是否可被滿足
     *
     * @param count 需求數量
     * @return 若剩餘庫存大於等於需求數量則返回 true
     */
    public boolean isAvailable(Integer count) {
        return count != null && count > 0 && this.residue != null && this.residue >= count;
    }

    /**
     * 業務重置：初始化或重置庫存總量 (重置 used 為 0，residue 為 total)
     *
     * @param newTotal 新總庫存數
     */
    public void reset(Integer newTotal) {
        if (newTotal == null || newTotal < 0) {
            throw new IllegalArgumentException("總庫存數不可為負數");
        }
        this.total = newTotal;
        this.used = 0;
        this.residue = newTotal;
    }
}
