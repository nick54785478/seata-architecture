package com.seata.demo.account.application.port.in;

import com.seata.demo.account.application.dto.query.GetAccountQuery;
import com.seata.demo.account.application.dto.result.AccountGottenResult;

/**
 * 查詢帳戶資產業務 UseCase Inbound Port (Get Account UseCase)
 * 命名規範 3-8: in : ... + UseCase
 * 定義查詢用戶可用餘額與凍結金額之業務合約。
 *
 * @author Seata Architecture Demo Team
 */
public interface GetAccountUseCase {

    /**
     * 依用戶 ID 查詢帳戶資產詳情
     *
     * @param query 查詢條件物件
     * @return 帳戶查詢結果 DTO
     */
    AccountGottenResult getAccount(GetAccountQuery query);
}
