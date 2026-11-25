package com.seata.demo.account.application.service;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.seata.demo.account.application.dto.command.PrepareAccountPaymentCommand;
import com.seata.demo.account.application.port.out.AccountRepositoryPort;
import com.seata.demo.account.application.port.out.TccRecordRepositoryPort;
import com.seata.demo.account.domain.account.aggregate.root.Account;
import com.seata.demo.account.domain.account.aggregate.vo.Money;
import com.seata.demo.account.domain.account.aggregate.vo.UserId;
import com.seata.demo.account.domain.account.exception.BalanceInsufficientException;
import com.seata.demo.shared.constant.TccStatus;

/**
 * TCC 預備帳戶支付案例單元測試 (Prepare Account Payment UseCase Unit Test)
 * 遵循規範 5: 使用 BDD 與 Gherkin 語法撰寫測試描述，涵蓋正常路徑與防懸掛/異常路徑。
 *
 */
@ExtendWith(MockitoExtension.class)
class PrepareAccountPaymentUseCaseTest {

        @Mock
        private AccountRepositoryPort accountRepositoryPort;

        @Mock
        private TccRecordRepositoryPort tccRecordRepositoryPort;

        private AccountApplicationService accountApplicationService;

        @BeforeEach
        void setUp() {
                accountApplicationService = new AccountApplicationService(accountRepositoryPort,
                                tccRecordRepositoryPort);
        }

        /**
         * Feature: 帳戶資金預留業務 (TCC Try)
         * Scenario: 當可用餘額充足且無懸掛風險時，成功凍結金額
         * Given 用戶 ID 為 10001，可用餘額為 10000.00，凍結金額為 0.00
         * And 該全局事務 (XID) 尚未收到過 Cancel 指令
         * When 接收到凍結 500.00 元之 Try 命令
         * Then 可用餘額變更為 9500.00，凍結金額變更為 500.00，並成功記錄狀態為 TRIED
         */
        @Test
        @DisplayName("情境：餘額充足時 Try 成功凍結資金")
        void givenSufficientBalance_whenPrepare_thenSuccess() {
                // Given
                Long userId = 10001L;
                String xid = "TX_1001";
                Long branchId = 1L;
                BigDecimal amount = new BigDecimal("500.00");

                Account account = new Account(
                                UserId.of(userId),
                                Money.of(new BigDecimal("10000.00")),
                                Money.zero());

                Mockito.when(tccRecordRepositoryPort.findStatusByXidAndBranchId(xid, branchId))
                                .thenReturn(Optional.empty());
                Mockito.when(accountRepositoryPort.findByUserId(UserId.of(userId)))
                                .thenReturn(Optional.of(account));

                PrepareAccountPaymentCommand command = PrepareAccountPaymentCommand.builder()
                                .xid(xid)
                                .branchId(branchId)
                                .userId(userId)
                                .amount(amount)
                                .build();

                // When
                boolean result = accountApplicationService.preparePayment(command);

                // Then
                Assertions.assertTrue(result);
                Assertions.assertEquals(new BigDecimal("9500.00"), account.getBalance().getValue());
                Assertions.assertEquals(new BigDecimal("500.00"), account.getFreezeAmount().getValue());
                Mockito.verify(accountRepositoryPort, Mockito.times(1)).save(account);
                Mockito.verify(tccRecordRepositoryPort, Mockito.times(1)).saveRecord(xid, branchId, userId,
                                TccStatus.TRIED, amount);
        }

        /**
         * Feature: 帳戶資金預留業務 (TCC Try)
         * Scenario: 當 Cancel 先於 Try 到達時，觸發防懸掛機制拒絕 Try 執行
         * Given 該事務分支已經被記錄為 CANCELED 狀態
         * When 接收到延遲到達的 Try 命令
         * Then 系統直接返回 false，拒絕扣減可用餘額，防止業務懸掛
         */
        @Test
        @DisplayName("情境：檢測到已存在 Cancel 記錄時觸發防懸掛，拒絕 Try 執行")
        void givenCanceledRecord_whenPrepare_thenPreventSuspension() {
                // Given
                String xid = "TX_1002";
                Long branchId = 2L;

                Mockito.when(tccRecordRepositoryPort.findStatusByXidAndBranchId(xid, branchId))
                                .thenReturn(Optional.of(TccStatus.CANCELED));

                PrepareAccountPaymentCommand command = PrepareAccountPaymentCommand.builder()
                                .xid(xid)
                                .branchId(branchId)
                                .userId(10001L)
                                .amount(new BigDecimal("500.00"))
                                .build();

                // When
                boolean result = accountApplicationService.preparePayment(command);

                // Then
                Assertions.assertFalse(result);
                Mockito.verify(accountRepositoryPort, Mockito.never()).save(Mockito.any());
        }

        /**
         * Feature: 帳戶資金預留業務 (TCC Try)
         * Scenario: 當可用餘額不足時，拋出領域例外
         * Given 用戶可用餘額為 100.00
         * When 接收到凍結 500.00 之 Try 命令
         * Then 系統拋出 BalanceInsufficientException 領域例外
         */
        @Test
        @DisplayName("情境：可用餘額不足時拋出 BalanceInsufficientException")
        void givenInsufficientBalance_whenPrepare_thenThrowException() {
                // Given
                Long userId = 10001L;
                String xid = "TX_1003";
                Long branchId = 3L;

                Account account = new Account(
                                UserId.of(userId),
                                Money.of(new BigDecimal("100.00")),
                                Money.zero());

                Mockito.when(tccRecordRepositoryPort.findStatusByXidAndBranchId(xid, branchId))
                                .thenReturn(Optional.empty());
                Mockito.when(accountRepositoryPort.findByUserId(UserId.of(userId)))
                                .thenReturn(Optional.of(account));

                PrepareAccountPaymentCommand command = PrepareAccountPaymentCommand.builder()
                                .xid(xid)
                                .branchId(branchId)
                                .userId(userId)
                                .amount(new BigDecimal("500.00"))
                                .build();

                // When & Then
                Assertions.assertThrows(BalanceInsufficientException.class, () -> {
                        accountApplicationService.preparePayment(command);
                });
        }
}
