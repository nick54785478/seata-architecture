package com.seata.demo.order.application.dto.command;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 建立訂單命令 (Create Order Command)
 * 命名規範 3-4: V + N + Command
 * 純資料載體，不含業務轉換邏輯。
 *
 * @author Seata Architecture Demo Team
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderCommand {

    private Long userId;
    private Long productId;
    private Integer count;
    private BigDecimal payAmount;
    private Boolean simulateError;
}
