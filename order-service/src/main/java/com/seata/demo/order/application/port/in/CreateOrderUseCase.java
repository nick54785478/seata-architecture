package com.seata.demo.order.application.port.in;

import com.seata.demo.order.application.dto.command.CreateOrderCommand;
import com.seata.demo.order.application.dto.result.OrderGottenResult;

/**
 * 建立訂單案例 Inbound Port 介面 (Create Order UseCase)
 * 命名規範 3-8: ... + UseCase
 */
public interface CreateOrderUseCase {

    /**
     * 執行下單全鏈路業務流程 (發起 Seata 全局事務)
     *
     * @param command 建立訂單命令
     * @return 訂單結果 DTO
     */
    OrderGottenResult createOrder(CreateOrderCommand command);
}
