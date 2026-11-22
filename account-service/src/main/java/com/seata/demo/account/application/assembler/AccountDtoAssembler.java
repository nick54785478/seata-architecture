package com.seata.demo.account.application.assembler;

import com.seata.demo.account.application.dto.result.AccountGottenResult;
import com.seata.demo.account.domain.account.aggregate.root.Account;

/**
 * 帳戶 DTO 轉換器 (Account DTO Assembler)
 * 位於 Application 層，嚴守單一職責原則，負責領域模型與 DTO 的映射轉換。
 *
 * @author Seata Architecture Demo Team
 */
public final class AccountDtoAssembler {

    private AccountDtoAssembler() {}

    /**
     * 將 Account 領域聚合根轉換為 AccountGottenResult DTO
     *
     * @param account 領域聚合根
     * @return DTO 資料載體
     */
    public static AccountGottenResult toResult(Account account) {
        if (account == null) {
            return null;
        }
        return AccountGottenResult.builder()
                .userId(account.getUserId().getValue())
                .balance(account.getBalance().getValue())
                .freezeAmount(account.getFreezeAmount().getValue())
                .build();
    }
}
