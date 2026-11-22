package com.seata.demo.shared.constant;

/**
 * TCC 事務二階段狀態常數 (TCC Action Status)
 * 用於防止空回滾、防業務懸掛與二階段冪等控制
 *
 * @author Seata Architecture Demo Team
 */
public enum TccStatus {

    /**
     * 一階段 Try 成功：資金或資源已凍結
     */
    TRIED,

    /**
     * 二階段 Confirm 成功：資源已正式扣減完成
     */
    CONFIRMED,

    /**
     * 二階段 Cancel 成功：凍結資源已退回解凍
     */
    CANCELED
}
