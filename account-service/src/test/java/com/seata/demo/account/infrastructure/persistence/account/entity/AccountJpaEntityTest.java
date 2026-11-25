package com.seata.demo.account.infrastructure.persistence.account.entity;

import java.math.BigDecimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 帳戶 JPA 充血模型實體單元測試 (AccountJpaEntity Test)
 * 驗證 TCC 二階段凍結、劃轉、解凍業務邏輯與餘額不足防禦
 */
@DisplayName("帳戶 JPA 充血模型實體單元測試")
class AccountJpaEntityTest {

    /**
     * Feature: 帳戶充血實體建立
     * Scenario: 建立新帳戶時，餘額正確賦值且凍結款項為 0.00
     */
    @Test
    @DisplayName("情境：正確建立帳戶充血實體")
    void givenValidParams_whenCreate_thenSuccess() {
        AccountJpaEntity account = AccountJpaEntity.create(10001L, new BigDecimal("1000.00"));

        Assertions.assertNotNull(account);
        Assertions.assertEquals(10001L, account.getUserId());
        Assertions.assertEquals(0, account.getBalance().compareTo(new BigDecimal("1000.00")));
        Assertions.assertEquals(0, account.getFreezeAmount().compareTo(BigDecimal.ZERO));
        Assertions.assertTrue(account.hasSufficientBalance(new BigDecimal("500.00")));
        Assertions.assertFalse(account.hasSufficientBalance(new BigDecimal("1500.00")));
    }

    /**
     * Feature: TCC 一階段資金凍結 (Try)
     * Scenario: 可用餘額充足時，成功扣減可用餘額並增加凍結款項
     */
    @Test
    @DisplayName("情境：可用餘額充足時成功凍結款項")
    void givenSufficientBalance_whenFreeze_thenSuccess() {
        AccountJpaEntity account = AccountJpaEntity.create(10001L, new BigDecimal("1000.00"));

        account.freeze(new BigDecimal("300.00"));

        Assertions.assertEquals(0, account.getBalance().compareTo(new BigDecimal("700.00")));
        Assertions.assertEquals(0, account.getFreezeAmount().compareTo(new BigDecimal("300.00")));
    }

    /**
     * Feature: 餘額不足防禦
     * Scenario: 可用餘額小於欲凍結金額時拋出 IllegalStateException
     */
    @Test
    @DisplayName("情境：可用餘額不足時拒絕凍結並拋出例外")
    void givenInsufficientBalance_whenFreeze_thenThrowException() {
        AccountJpaEntity account = AccountJpaEntity.create(10001L, new BigDecimal("100.00"));

        Assertions.assertThrows(IllegalStateException.class, () ->
                account.freeze(new BigDecimal("200.00")));
    }

    /**
     * Feature: TCC 二階段正式劃扣 (Confirm)
     * Scenario: 正式自凍結資金中扣除款項
     */
    @Test
    @DisplayName("情境：二階段 Confirm 正式劃扣凍結資金")
    void givenFrozenFunds_whenConfirm_thenSuccess() {
        AccountJpaEntity account = AccountJpaEntity.create(10001L, new BigDecimal("1000.00"));
        account.freeze(new BigDecimal("300.00"));

        account.confirm(new BigDecimal("300.00"));

        Assertions.assertEquals(0, account.getBalance().compareTo(new BigDecimal("700.00")));
        Assertions.assertEquals(0, account.getFreezeAmount().compareTo(BigDecimal.ZERO));
    }

    /**
     * Feature: TCC 二階段解凍退款 (Cancel)
     * Scenario: 解凍款項自凍結金額扣除並原路退回可用餘額
     */
    @Test
    @DisplayName("情境：二階段 Cancel 解凍款項退回可用餘額")
    void givenFrozenFunds_whenCancel_thenSuccess() {
        AccountJpaEntity account = AccountJpaEntity.create(10001L, new BigDecimal("1000.00"));
        account.freeze(new BigDecimal("300.00"));

        account.cancel(new BigDecimal("300.00"));

        Assertions.assertEquals(0, account.getBalance().compareTo(new BigDecimal("1000.00")));
        Assertions.assertEquals(0, account.getFreezeAmount().compareTo(BigDecimal.ZERO));
    }

    /**
     * Feature: 帳戶資產業務重置
     * Scenario: 呼叫 reset 重置餘額，凍結金額歸零
     */
    @Test
    @DisplayName("情境：重置帳戶資產至基準餘額")
    void givenMutatedAccount_whenReset_thenSuccess() {
        AccountJpaEntity account = AccountJpaEntity.create(10001L, new BigDecimal("1000.00"));
        account.freeze(new BigDecimal("300.00"));

        account.reset(new BigDecimal("5000.00"));

        Assertions.assertEquals(0, account.getBalance().compareTo(new BigDecimal("5000.00")));
        Assertions.assertEquals(0, account.getFreezeAmount().compareTo(BigDecimal.ZERO));
    }
}
