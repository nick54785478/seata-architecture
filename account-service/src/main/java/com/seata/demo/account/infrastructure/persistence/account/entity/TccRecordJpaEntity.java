package com.seata.demo.account.infrastructure.persistence.account.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * TCC 控制表記錄充血模型實體 (TCC Record JPA Rich Entity)
 * 位於 Infrastructure 層持久化模組，專責 Seata TCC 防懸掛 (Anti-Suspension)、防空回滾 (Anti-Empty-Rollback)
 * 與二階段冪等性 (Idempotency) 控制表記錄維護。
 * <p>
 * <b>【充血模型設計特性 (Rich Domain Model)】</b>
 * <ul>
 *   <li><b>防禦外部 Setter</b>：移除公開 Setter，狀態機推進由專屬業務方法控管。</li>
 *   <li><b>狀態判斷內聚</b>：內建 {@link #isTried()}、{@link #isConfirmed()}、{@link #isCanceled()} 判定。</li>
 *   <li><b>自動審計欄位</b>：在工廠方法中自驅生成創建時間。</li>
 * </ul>
 */
@Entity
@Table(name = "t_tcc_transaction_record")
@IdClass(TccRecordCompositeKey.class)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TccRecordJpaEntity {

    @Id
    @Column(name = "xid", length = 128)
    private String xid;

    @Id
    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "freeze_amount", nullable = false)
    private BigDecimal freezeAmount;

    @Column(name = "gmt_create", nullable = false)
    private LocalDateTime gmtCreate;

    /**
     * 靜態工廠方法：建立 TCC 狀態控制記錄
     *
     * @param xid          全局事務 ID
     * @param branchId     分支事務 ID
     * @param userId       用戶 ID
     * @param status       TCC 初始狀態 (TRIED / CANCELED)
     * @param freezeAmount 凍結金額
     * @return TCC 控制記錄充血實體
     */
    public static TccRecordJpaEntity create(String xid, Long branchId, Long userId, String status, BigDecimal freezeAmount) {
        if (xid == null || xid.trim().isEmpty()) {
            throw new IllegalArgumentException("XID 不可為空");
        }
        if (branchId == null) {
            throw new IllegalArgumentException("Branch ID 不可為空");
        }
        return TccRecordJpaEntity.builder()
                .xid(xid)
                .branchId(branchId)
                .userId(userId)
                .status(status)
                .freezeAmount(freezeAmount != null ? freezeAmount : BigDecimal.ZERO)
                .gmtCreate(LocalDateTime.now())
                .build();
    }

    /**
     * 業務推進：更新 TCC 事務狀態
     *
     * @param newStatus 新目標狀態
     */
    public void transitionTo(String newStatus) {
        if (newStatus == null || newStatus.trim().isEmpty()) {
            throw new IllegalArgumentException("新狀態不可為空");
        }
        this.status = newStatus;
    }

    /**
     * 業務查詢：判斷是否為 Try 成功狀態
     */
    public boolean isTried() {
        return "TRIED".equals(this.status);
    }

    /**
     * 業務查詢：判斷是否已確認提交 (Confirm)
     */
    public boolean isConfirmed() {
        return "CONFIRMED".equals(this.status);
    }

    /**
     * 業務查詢：判斷是否已回滾取消或防懸掛標記 (Cancel)
     */
    public boolean isCanceled() {
        return "CANCELED".equals(this.status);
    }
}
