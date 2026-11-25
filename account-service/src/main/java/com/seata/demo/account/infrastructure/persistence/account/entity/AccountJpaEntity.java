package com.seata.demo.account.infrastructure.persistence.account.entity;

import java.math.BigDecimal;
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
 * 帳戶資產 JPA ORM 充血模型實體 (Account JPA Rich Entity)
 * 位於 Infrastructure 層持久化模組，專責 MySQL 表結構映射與 TCC 二階段資產數值計算保護。
 * <p>
 * <b>【充血模型設計特性 (Rich Domain Model)】</b>
 * <ul>
 *   <li><b>防禦外部 Setter 竄改</b>：移除公開 Setter，資產變動完全內聚於原子操作。</li>
 *   <li><b>TCC 兩階段資產約束</b>：內建 {@link #freeze(BigDecimal)}、{@link #confirm(BigDecimal)}、{@link #cancel(BigDecimal)}。</li>
 *   <li><b>資產守恆校驗</b>：確保扣款、凍結與解凍流程中不會出現負餘額或非法超扣。</li>
 * </ul>
 */
@Entity
@Table(name = "t_account")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AccountJpaEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance;

    @Column(name = "freeze_amount", nullable = false)
    private BigDecimal freezeAmount;

    /**
     * 靜態工廠方法：建立新帳戶記錄
     *
     * @param userId         用戶 ID
     * @param initialBalance 初始可用餘額
     * @return 帳戶充血實體 (凍結款項預設為 0.00)
     */
    public static AccountJpaEntity create(Long userId, BigDecimal initialBalance) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("用戶 ID 不可為空且必須大於 0");
        }
        if (initialBalance == null || initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("初始可用餘額不可為負數");
        }
        return AccountJpaEntity.builder()
                .userId(userId)
                .balance(initialBalance)
                .freezeAmount(BigDecimal.ZERO)
                .build();
    }

    /**
     * TCC 一階段業務行為：凍結資金 (Try)
     * 從可用餘額中扣減，並加入至凍結金額中
     *
     * @param amount 預扣凍結金額
     * @throws IllegalArgumentException 傳入金額非正數時拋出
     * @throws IllegalStateException    可用餘額不足時拋出
     */
    public void freeze(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (!hasSufficientBalance(amount)) {
            throw new IllegalStateException(String.format(
                    "用戶 ID: %d 可用餘額不足！當前餘額: %s, 欲凍結金額: %s", userId, balance, amount));
        }
        this.balance = this.balance.subtract(amount);
        this.freezeAmount = this.freezeAmount.add(amount);
    }

    /**
     * TCC 二階段業務行為：正式劃轉 (Confirm)
     * 從凍結資金中扣除款項完成劃款
     *
     * @param amount 正式扣劃金額
     * @throws IllegalArgumentException 傳入金額非正數時拋出
     * @throws IllegalStateException    凍結資金小於劃款金額時拋出
     */
    public void confirm(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (this.freezeAmount.compareTo(amount) < 0) {
            throw new IllegalStateException(String.format(
                    "用戶 ID: %d 凍結金額不足以劃轉！凍結款項: %s, 欲劃扣: %s", userId, freezeAmount, amount));
        }
        this.freezeAmount = this.freezeAmount.subtract(amount);
    }

    /**
     * TCC 二階段業務行為：解凍退回 (Cancel)
     * 釋放凍結資金並原路退回至可用餘額中
     *
     * @param amount 解凍款項
     * @throws IllegalArgumentException 傳入金額非正數時拋出
     * @throws IllegalStateException    凍結資金小於解凍金額時拋出
     */
    public void cancel(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (this.freezeAmount.compareTo(amount) < 0) {
            throw new IllegalStateException(String.format(
                    "用戶 ID: %d 凍結金額不足以解凍！凍結款項: %s, 欲解凍: %s", userId, freezeAmount, amount));
        }
        this.freezeAmount = this.freezeAmount.subtract(amount);
        this.balance = this.balance.add(amount);
    }

    /**
     * 業務查詢：檢查帳戶可用餘額是否充足
     *
     * @param amount 欲支出金額
     * @return 若可用餘額大於等於需求金額則回傳 true
     */
    public boolean hasSufficientBalance(BigDecimal amount) {
        return amount != null && this.balance != null && this.balance.compareTo(amount) >= 0;
    }

    /**
     * 業務重置：初始化或重置帳戶餘額 (重置 freezeAmount 為 0)
     *
     * @param initialBalance 新可用餘額
     */
    public void reset(BigDecimal initialBalance) {
        if (initialBalance == null || initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("帳戶重置餘額不可為負數");
        }
        this.balance = initialBalance;
        this.freezeAmount = BigDecimal.ZERO;
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("操作金額必須為大於 0 的正數");
        }
    }
}
