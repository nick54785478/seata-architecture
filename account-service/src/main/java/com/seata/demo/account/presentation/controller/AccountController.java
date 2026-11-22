package com.seata.demo.account.presentation.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.seata.demo.account.application.dto.query.GetAccountQuery;
import com.seata.demo.account.application.dto.result.AccountGottenResult;
import com.seata.demo.account.application.port.in.GetAccountUseCase;
import com.seata.demo.account.application.tcc.AccountTccAction;
import com.seata.demo.account.presentation.assembler.AccountResourceAssembler;
import com.seata.demo.account.presentation.resource.in.PrepareAccountPaymentResource;
import com.seata.demo.account.presentation.resource.out.AccountPaymentPreparedResource;
import com.seata.demo.account.presentation.resource.out.AccountRetrievedResource;
import com.seata.demo.shared.response.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 帳戶表現層 REST 控制器 (Account REST Controller)
 * 包含 TCC 一階段 Try HTTP 介面與帳戶資產業務查詢介面。
 *
 * @author Seata Architecture Demo Team
 */
@Tag(name = "帳戶服務 API (Account TCC Service)", description = "提供帳戶資產管理與業務查詢，作為 Seata TCC 模式資源管理器 (RM)，實作兩階段自定義 Try-Confirm-Cancel")
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountTccAction accountTccAction;
    private final GetAccountUseCase getAccountUseCase;

    public AccountController(AccountTccAction accountTccAction, GetAccountUseCase getAccountUseCase) {
        this.accountTccAction = accountTccAction;
        this.getAccountUseCase = getAccountUseCase;
    }

    /**
     * TCC 一階段 Try: 預留/凍結帳戶資金 API
     *
     * @param resource 請求載體
     * @return 統一回應
     */
    @Operation(summary = "預留/凍結帳戶資金 (TCC 一階段 Try)", description = "驗證帳戶餘額並將扣款金額轉移至凍結金額 (freeze_amount)，支援防懸掛、防空回滾與冪等性檢查。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "資金凍結成功"),
            @ApiResponse(responseCode = "400", description = "參數錯誤或餘額不足 (AccountBalanceNotEnoughException)"),
            @ApiResponse(responseCode = "404", description = "找不到該用戶帳戶 (AccountNotFoundException)")
    })
    @PostMapping("/tcc/prepare")
    public Result<AccountPaymentPreparedResource> preparePayment(@Validated @RequestBody PrepareAccountPaymentResource resource) {
        boolean success;
        try {
            success = accountTccAction.prepare(null, resource.getUserId(), resource.getAmount());
        } catch (RuntimeException ex) {
            // Seata TCC 代理切面 (AdapterInvocationWrapper) 會將 Try 拋出的領域例外包裝成 RuntimeException("try to proceed invocation error", cause)
            // 此處將真實的底層例外拆箱拋出，以利表現層與全域例外處理器精準捕捉業務語意
            if (ex.getCause() instanceof RuntimeException runtimeCause) {
                throw runtimeCause;
            }
            throw ex;
        }

        if (!success) {
            return Result.failure("資金凍結失敗或已被取消！");
        }
        AccountPaymentPreparedResource response = AccountResourceAssembler.toResource(resource.getUserId(), resource.getAmount());
        return Result.success(response);
    }

    /**
     * 依據用戶 ID 查詢即時帳戶資產詳情 (業務查詢 API)
     *
     * @param userId 用戶 ID
     * @return 帳戶資產詳情統一回應
     */
    @Operation(summary = "依用戶 ID 查詢帳戶資產詳情 (業務查詢 API)", description = "取得用戶的可用餘額、TCC 凍結金額與總資產。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功"),
            @ApiResponse(responseCode = "404", description = "找不到該用戶帳戶 (AccountNotFoundException)")
    })
    @GetMapping("/{userId}")
    public Result<AccountRetrievedResource> getAccount(@PathVariable Long userId) {
        GetAccountQuery query = GetAccountQuery.builder().userId(userId).build();
        AccountGottenResult result = getAccountUseCase.getAccount(query);
        AccountRetrievedResource response = AccountResourceAssembler.toRetrievedResource(result);
        return Result.success(response);
    }
}
