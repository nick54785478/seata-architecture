-- ========================================================
-- Seata Client AT 模式 Undo Log 表結構 (MySQL 5.7+ / 8.0+)
-- 需在所有參與 AT 模式分支事務的業務資料庫中執行
-- ========================================================

CREATE TABLE IF NOT EXISTS `undo_log` (
  `branch_id` BIGINT NOT NULL COMMENT '分支事務ID',
  `xid` VARCHAR(128) NOT NULL COMMENT '全局事務ID',
  `context` VARCHAR(128) NOT NULL COMMENT '上下文資訊',
  `rollback_info` LONGBLOB NOT NULL COMMENT '回滾信息 (前後鏡像快照)',
  `log_status` INT NOT NULL COMMENT '狀態 (0: 正常, 1: 防懸掛已回滾)',
  `log_created` DATETIME(6) NOT NULL COMMENT '創建時間',
  `log_modified` DATETIME(6) NOT NULL COMMENT '修改時間',
  UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`),
  INDEX `ix_log_created` (`log_created`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Seata AT模式事務復原日誌表';
