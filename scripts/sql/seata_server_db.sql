-- ========================================================
-- Seata Server (TC) DB 模式儲存結構 (MySQL 5.7+ / 8.0+)
-- 適用於 TC 採用 db 模式持久化狀態時使用
-- ========================================================

CREATE DATABASE IF NOT EXISTS `seata_server` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `seata_server`;

-- 1. 全局事務狀態表
CREATE TABLE IF NOT EXISTS `global_table` (
  `xid` VARCHAR(128) NOT NULL COMMENT '全局事務唯一ID',
  `transaction_id` BIGINT COMMENT '事務ID',
  `status` TINYINT NOT NULL COMMENT '事務狀態',
  `application_id` VARCHAR(64) COMMENT '應用ID',
  `transaction_service_group` VARCHAR(64) COMMENT '事務服務群組',
  `transaction_name` VARCHAR(128) COMMENT '事務名稱',
  `timeout` INT COMMENT '超時時間 (ms)',
  `begin_time` BIGINT COMMENT '開始時間戳',
  `application_data` VARCHAR(2000) COMMENT '自定義應用數據',
  `gmt_create` DATETIME COMMENT '創建時間',
  `gmt_modified` DATETIME COMMENT '更新時間',
  PRIMARY KEY (`xid`),
  KEY `idx_status_gmt_modified` (`status`, `gmt_modified`),
  KEY `idx_transaction_id` (`transaction_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '全局事務狀態表記錄';

-- 2. 分支事務狀態表
CREATE TABLE IF NOT EXISTS `branch_table` (
  `branch_id` BIGINT NOT NULL COMMENT '分支事務ID',
  `xid` VARCHAR(128) NOT NULL COMMENT '全局事務ID',
  `transaction_id` BIGINT COMMENT '事務ID',
  `resource_group_id` VARCHAR(32) COMMENT '資源分組ID',
  `resource_id` VARCHAR(256) COMMENT '資源ID (如 jdbc-url)',
  `branch_type` VARCHAR(8) COMMENT '分支類型 (AT, TCC, SAGA, XA)',
  `status` TINYINT COMMENT '分支事務狀態',
  `client_id` VARCHAR(64) COMMENT '客戶端唯一標識',
  `application_data` VARCHAR(2000) COMMENT '應用數據',
  `gmt_create` DATETIME(6) COMMENT '創建時間',
  `gmt_modified` DATETIME(6) COMMENT '更新時間',
  PRIMARY KEY (`branch_id`),
  KEY `idx_xid` (`xid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '分支事務狀態表記錄';

-- 3. 全局行鎖表 (用於 AT 模式防髒寫)
CREATE TABLE IF NOT EXISTS `lock_table` (
  `row_key` VARCHAR(128) NOT NULL COMMENT '行鎖標識 (resource_id^^^table_name^^^pk)',
  `xid` VARCHAR(128) COMMENT '持有該鎖的全局事務ID',
  `transaction_id` BIGINT COMMENT '事務ID',
  `branch_id` BIGINT NOT NULL COMMENT '分支事務ID',
  `resource_id` VARCHAR(256) COMMENT '資源ID',
  `table_name` VARCHAR(32) COMMENT '表名',
  `pk` VARCHAR(36) COMMENT '主鍵值',
  `status` TINYINT NOT NULL DEFAULT '0' COMMENT '狀態 (0:鎖定, 1:可釋放)',
  `gmt_create` DATETIME COMMENT '創建時間',
  `gmt_modified` DATETIME COMMENT '更新時間',
  PRIMARY KEY (`row_key`),
  KEY `idx_status` (`status`),
  KEY `idx_branch_id` (`branch_id`),
  KEY `idx_xid` (`xid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '全局行鎖表記錄';

-- 4. 分布式排他鎖表 (Leader 選舉/併發控制)
CREATE TABLE IF NOT EXISTS `distributed_lock` (
  `lock_key` CHAR(20) NOT NULL COMMENT '鎖鍵名',
  `lock_value` VARCHAR(20) NOT NULL COMMENT '鎖值',
  `expire` BIGINT NOT NULL COMMENT '過期時間戳',
  PRIMARY KEY (`lock_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Seata 內部分布式鎖';

INSERT IGNORE INTO `distributed_lock` (`lock_key`, `lock_value`, `expire`) VALUES ('AsyncCommitting', ' ', 0);
INSERT IGNORE INTO `distributed_lock` (`lock_key`, `lock_value`, `expire`) VALUES ('RetryCommitting', ' ', 0);
INSERT IGNORE INTO `distributed_lock` (`lock_key`, `lock_value`, `expire`) VALUES ('RetryRollbacking', ' ', 0);
INSERT IGNORE INTO `distributed_lock` (`lock_key`, `lock_value`, `expire`) VALUES ('TxTimeoutCheck', ' ', 0);
