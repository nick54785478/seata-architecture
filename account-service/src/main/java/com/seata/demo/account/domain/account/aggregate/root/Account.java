package com.seata.demo.account.domain.account.aggregate.root;

import com.seata.demo.account.domain.account.aggregate.vo.Money;
import com.seata.demo.account.domain.account.aggregate.vo.UserId;
import com.seata.demo.account.domain.account.event.MoneyDeductedEvent;
import com.seata.demo.account.domain.account.event.MoneyFrozenEvent;
import com.seata.demo.account.domain.account.event.MoneyUnfrozenEvent;
import com.seata.demo.account.domain.account.exception.BalanceInsufficientException;

/**
 * 帳戶聚合根 (Account Aggregate Root)
 * 純 Java 實作，遵循 DDD 規範：嚴禁使用 Lombok 註解，屬性完全封裝，透過具備業務語意的行為方法進行異動。
 * 支援 TCC 模式二階段資金凍結、正式劃扣與解凍回退。
 *
 * @author Seata Architecture Demo Team
 */
public class Account {

    private final UserId userId;
    private Money balance;
    private Money freezeAmount;

    public Account(UserId userId, Money balance, Money freezeAmount) {
        this.userId = userId;
        this.balance = balance;
        this.freezeAmount = freezeAmount;
    }

    /**
     * TCC 一階段業務行為：凍結資金 (Try / Prepare)
     *
     * @param amount 預期凍結金額
     * @return 資金凍結領域事件
     * @throws BalanceInsufficientException 可用餘額不足時拋出
     */
    public MoneyFrozenEvent freeze(Money amount) {
        if (!this.balance.isGreaterThanOrEqualTo(amount)) {
            throw new BalanceInsufficientException(this.userId.getValue(), amount.getValue(), this.balance.getValue());
        }

        this.balance = this.balance.subtract(amount);
        this.freezeAmount = this.freezeAmount.add(amount);

        return new MoneyFrozenEvent(this.userId.getValue(), amount.getValue(), this.balance.getValue());
    }

    /**
     * TCC 二階段業務行為：正式劃轉扣減凍結資金 (Confirm)
     *
     * @param amount 劃轉金額
     * @return 資金扣減領域事件
     */
    public MoneyDeductedEvent deductFrozen(Money amount) {
        this.freezeAmount = this.freezeAmount.subtract(amount);
        return new MoneyDeductedEvent(this.userId.getValue(), amount.getValue());
    }

    /**
     * TCC 二階段業務行為：解凍資金退回可用餘額 (Cancel)
     *
     * @param amount 解凍金額
     * @return 資金解凍領域事件
     */
    public MoneyUnfrozenEvent unfreeze(Money amount) {
        this.freezeAmount = this.freezeAmount.subtract(amount);
        this.balance = this.balance.add(amount);
        return new MoneyUnfrozenEvent(this.userId.getValue(), amount.getValue(), this.balance.getValue());
    }

    public UserId getUserId() {
        return userId;
    }

    public Money getBalance() {
        return balance;
    }

    public Money getFreezeAmount() {
        return freezeAmount;
    }
}
