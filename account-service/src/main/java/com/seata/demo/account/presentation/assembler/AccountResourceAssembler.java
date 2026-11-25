package com.seata.demo.account.presentation.assembler;

import java.math.BigDecimal;
import com.seata.demo.account.application.dto.command.PrepareAccountPaymentCommand;
import com.seata.demo.account.presentation.resource.in.PrepareAccountPaymentResource;
import com.seata.demo.account.presentation.resource.out.AccountPaymentPreparedResource;

/**
 * 帳戶表現層資源轉換器 (Account Resource Assembler)
 */
public final class AccountResourceAssembler {

    private AccountResourceAssembler() {
    }

    public static PrepareAccountPaymentCommand toCommand(String xid, Long branchId,
            PrepareAccountPaymentResource resource) {
        if (resource == null) {
            return null;
        }
        return PrepareAccountPaymentCommand.builder()
                .xid(xid)
                .branchId(branchId)
                .userId(resource.getUserId())
                .amount(resource.getAmount())
                .build();
    }

    public static AccountPaymentPreparedResource toResource(Long userId, BigDecimal amount) {
        return AccountPaymentPreparedResource.builder()
                .userId(userId)
                .frozenAmount(amount)
                .status("SUCCESS")
                .build();
    }

    /**
     * 將應用層 DTO 轉換為帳戶資產查詢回應載體
     *
     * @param result 帳戶查詢結果 DTO
     * @return 帳戶查詢回應載體
     */
    public static com.seata.demo.account.presentation.resource.out.AccountRetrievedResource toRetrievedResource(
            com.seata.demo.account.application.dto.result.AccountGottenResult result) {
        if (result == null) {
            return null;
        }
        BigDecimal balance = result.getBalance() != null ? result.getBalance() : BigDecimal.ZERO;
        BigDecimal freezeAmount = result.getFreezeAmount() != null ? result.getFreezeAmount() : BigDecimal.ZERO;
        return com.seata.demo.account.presentation.resource.out.AccountRetrievedResource.builder()
                .userId(result.getUserId())
                .balance(balance)
                .freezeAmount(freezeAmount)
                .totalAmount(balance.add(freezeAmount))
                .build();
    }
}
