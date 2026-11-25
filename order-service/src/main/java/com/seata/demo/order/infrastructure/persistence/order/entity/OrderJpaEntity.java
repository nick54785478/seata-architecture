package com.seata.demo.order.infrastructure.persistence.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 訂單 JPA ORM 充血模型實體 (Order JPA Rich Entity)
 * 位於 Infrastructure 層持久化模組，封裝訂單持久化欄位與生命週期狀態流轉業務邏輯。
 * <p>
 * <b>【充血模型設計特性 (Rich Domain Model)】</b>
 * <ul>
 *   <li><b>防禦外部無差別 Setter</b>：移除公開 Setter，狀態異動嚴格經由具備業務意圖的方法推進。</li>
 *   <li><b>訂單狀態機內聚</b>：透過 {@link #markAsSuccess()} 與 {@link #markAsFailed()} 管理狀態轉換。</li>
 *   <li><b>業務約束校驗</b>：在工廠方法中對金額與數量進行底層一致性約束校驗。</li>
 * </ul>
 */
@Entity
@Table(name = "t_order")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OrderJpaEntity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "count", nullable = false)
    private Integer count;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "gmt_create")
    private LocalDateTime gmtCreate;

    @Column(name = "gmt_modified")
    private LocalDateTime gmtModified;

    /**
     * 靜態工廠方法：建立新訂單持久化實體
     *
     * @param id        分散式雪花主鍵
     * @param userId    用戶 ID
     * @param productId 商品 ID
     * @param count     購買數量
     * @param amount    訂單金額
     * @return 訂單充血實體 (狀態為 CREATED)
     */
    public static OrderJpaEntity create(Long id, Long userId, Long productId, Integer count, BigDecimal amount) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("訂單主鍵 ID 必須為大於 0 的合法識別碼");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用戶 ID 不可為空且必須大於 0");
        }
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("商品 ID 不可為空且必須大於 0");
        }
        if (count == null || count <= 0) {
            throw new IllegalArgumentException("購買數量必須大於 0");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("訂單金額必須大於 0");
        }

        return OrderJpaEntity.builder()
                .id(id)
                .userId(userId)
                .productId(productId)
                .count(count)
                .amount(amount)
                .status("CREATED")
                .gmtCreate(LocalDateTime.now())
                .gmtModified(LocalDateTime.now())
                .build();
    }

    /**
     * 業務推進：標記訂單處理成功 (SUCCESS)
     */
    public void markAsSuccess() {
        this.status = "SUCCESS";
        this.gmtModified = LocalDateTime.now();
    }

    /**
     * 業務推進：標記訂單處理失敗或全局回滾 (FAILED)
     */
    public void markAsFailed() {
        this.status = "FAILED";
        this.gmtModified = LocalDateTime.now();
    }

    /**
     * 業務查詢：判斷訂單是否處於建立中狀態
     *
     * @return 若狀態為 CREATED 則回傳 true
     */
    public boolean isCreated() {
        return "CREATED".equals(this.status);
    }

    /**
     * 業務查詢：判斷訂單是否成功
     *
     * @return 若狀態為 SUCCESS 則回傳 true
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(this.status);
    }

    /**
     * 業務查詢：判斷訂單是否失敗
     *
     * @return 若狀態為 FAILED 則回傳 true
     */
    public boolean isFailed() {
        return "FAILED".equals(this.status);
    }
}
