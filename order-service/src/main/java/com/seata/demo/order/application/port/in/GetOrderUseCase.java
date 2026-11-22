package com.seata.demo.order.application.port.in;

import com.seata.demo.order.application.dto.query.GetOrderQuery;
import com.seata.demo.order.application.dto.result.OrderGottenResult;

/**
 * 查詢訂單 Inbound Port 介面 (Get Order UseCase)
 * 命名規範 3-8: in : ... + UseCase
 *
 * @author Seata Architecture Demo Team
 */
public interface GetOrderUseCase {

    /**
     * 依據訂單識別碼查詢訂單資料
     *
     * @param query 查詢條件物件
     * @return 訂單查詢結果 DTO
     */
    OrderGottenResult getOrder(GetOrderQuery query);
}
