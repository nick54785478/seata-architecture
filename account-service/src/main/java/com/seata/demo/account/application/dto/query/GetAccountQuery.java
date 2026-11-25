package com.seata.demo.account.application.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 查詢帳戶資產業務 Query (Get Account Query)
 * 命名規範 3-5: V + N + Query
 * 位於 Application 層，純資料載體。
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetAccountQuery {

    /**
     * 用戶 ID
     */
    private Long userId;
}
