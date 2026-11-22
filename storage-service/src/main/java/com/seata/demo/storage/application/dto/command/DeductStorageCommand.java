package com.seata.demo.storage.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 扣減庫存命令 (Deduct Storage Command)
 * 命名規範 3-4: V + N + Command
 * 純資料載體，不含業務轉換邏輯。
 *
 * @author Seata Architecture Demo Team
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeductStorageCommand {

    private Long productId;
    private Integer count;
}
