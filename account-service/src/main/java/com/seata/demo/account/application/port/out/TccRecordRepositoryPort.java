package com.seata.demo.account.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import com.seata.demo.shared.constant.TccStatus;

/**
 * TCC 事務記錄儲存庫 Outbound Port (TCC Record Repository Port)
 * 命名規範 3-8: ... + Port
 * 負責管理 TCC 控制表記錄，提供防空回滾、防懸掛與冪等性校驗能力。
 */
public interface TccRecordRepositoryPort {

    /**
     * 查詢特定的 TCC 事務記錄狀態
     *
     * @param xid      全局事務 ID
     * @param branchId 分支事務 ID
     * @return 狀態 (Optional)
     */
    Optional<TccStatus> findStatusByXidAndBranchId(String xid, Long branchId);

    /**
     * 取得已凍結之金額
     *
     * @param xid      全局事務 ID
     * @param branchId 分支事務 ID
     * @return 凍結金額
     */
    BigDecimal getFreezeAmount(String xid, Long branchId);

    /**
     * 保存或更新 TCC 記錄狀態 (包含 userId)
     *
     * @param xid          全局事務 ID
     * @param branchId     分支事務 ID
     * @param userId       用戶 ID
     * @param status       TCC 狀態 (TRIED, CONFIRMED, CANCELED)
     * @param freezeAmount 凍結金額
     */
    void saveRecord(String xid, Long branchId, Long userId, TccStatus status, BigDecimal freezeAmount);

    /**
     * 保存或更新 TCC 記錄狀態
     *
     * @param xid          全局事務 ID
     * @param branchId     分支事務 ID
     * @param status       TCC 狀態 (TRIED, CONFIRMED, CANCELED)
     * @param freezeAmount 凍結金額
     */
    default void saveRecord(String xid, Long branchId, TccStatus status, BigDecimal freezeAmount) {
        saveRecord(xid, branchId, null, status, freezeAmount);
    }

    /**
     * 依據 XID 與 BranchId 查詢用戶 ID
     *
     * @param xid      全局事務 ID
     * @param branchId 分支事務 ID
     * @return 用戶 ID (Optional)
     */
    Optional<Long> findUserIdByXidAndBranchId(String xid, Long branchId);
}
