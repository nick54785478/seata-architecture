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

import com.seata.demo.account.application.dto.query.GetAccountQuery;
import com.seata.demo.account.application.dto.result.AccountGottenResult;
import com.seata.demo.account.application.port.out.AccountRepositoryPort;
import com.seata.demo.account.application.port.out.TccRecordRepositoryPort;
import com.seata.demo.account.domain.account.aggregate.root.Account;
import com.seata.demo.account.domain.account.aggregate.vo.Money;
import com.seata.demo.account.domain.account.aggregate.vo.UserId;
import com.seata.demo.account.domain.account.exception.AccountNotFoundException;

/**
 * 查詢帳戶資產業務案例單元測試 (Get Account UseCase Unit Test)
 * 遵循規範 5: 使用 BDD 與 Gherkin 語法撰寫測試描述，涵蓋正常查詢與查無記錄之異常分支。
 *
 */
@ExtendWith(MockitoExtension.class)
class GetAccountUseCaseTest {

    @Mock
    private AccountRepositoryPort accountRepositoryPort;

    @Mock
    private TccRecordRepositoryPort tccRecordRepositoryPort;

    private AccountApplicationService accountApplicationService;

    @BeforeEach
    void setUp() {
        accountApplicationService = new AccountApplicationService(accountRepositoryPort, tccRecordRepositoryPort);
    }

    /**
     * Feature: 帳戶資產業務查詢
     * Scenario: 當用戶帳戶存在時，成功取得可用餘額與凍結金額
     * Given 用戶 ID 為 10001，可用餘額 9500.00，凍結金額 0.00
     * When 接收到依據用戶 ID 查詢帳戶之 Query
     * Then 成功返回對應之 AccountGottenResult，數據完全一致
     */
    @Test
    @DisplayName("情境：用戶存在時正常取得帳戶資產詳情")
    void givenExistingAccount_whenGetAccount_thenSuccess() {
        // Given
        Long userId = 10001L;
        Account account = new Account(
                UserId.of(userId),
                Money.of(new BigDecimal("9500.00")),
                Money.of(BigDecimal.ZERO));

        Mockito.when(accountRepositoryPort.findByUserId(UserId.of(userId)))
                .thenReturn(Optional.of(account));

        GetAccountQuery query = GetAccountQuery.builder().userId(userId).build();

        // When
        AccountGottenResult result = accountApplicationService.getAccount(query);

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(userId, result.getUserId());
        Assertions.assertEquals(0, result.getBalance().compareTo(new BigDecimal("9500.00")));
        Assertions.assertEquals(0, result.getFreezeAmount().compareTo(BigDecimal.ZERO));
        Mockito.verify(accountRepositoryPort, Mockito.times(1)).findByUserId(UserId.of(userId));
    }

    /**
     * Feature: 帳戶資產業務查詢
     * Scenario: 當用戶帳戶不存在時，拋出 AccountNotFoundException 領域例外
     * Given 用戶 ID 為 99999，底層無此帳戶記錄
     * When 接收到依據用戶 ID 查詢帳戶之 Query
     * Then 拋出 AccountNotFoundException
     */
    @Test
    @DisplayName("情境：用戶不存在時拋出 AccountNotFoundException")
    void givenNonExistingAccount_whenGetAccount_thenThrowException() {
        // Given
        Long userId = 99999L;
        Mockito.when(accountRepositoryPort.findByUserId(UserId.of(userId)))
                .thenReturn(Optional.empty());

        GetAccountQuery query = GetAccountQuery.builder().userId(userId).build();

        // When & Then
        Assertions.assertThrows(AccountNotFoundException.class, () -> {
            accountApplicationService.getAccount(query);
        });
        Mockito.verify(accountRepositoryPort, Mockito.times(1)).findByUserId(UserId.of(userId));
    }
}
