-- ========================================================
-- 00. Seata 全套微服務與 TC 中間件資料庫一鍵初始化腳本
-- 包含：
--   1. seata_server (TC 事務協調器：global_table, branch_table, lock_table, distributed_lock)
--   2. seata_order (訂單服務：t_order, undo_log)
--   3. seata_storage (庫存服務：t_storage, undo_log)
--   4. seata_account (帳戶服務：t_account, t_tcc_transaction_record, undo_log)
-- ========================================================

-- ========================================================
-- 1. 建立所有資料庫
-- ========================================================
CREATE DATABASE IF NOT EXISTS `seata` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `seata_server` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `seata_order` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `seata_storage` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `seata_account` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ========================================================
-- 2. 初始化 seata_server (TC 事務協調器狀態庫)
-- ========================================================
USE `seata_server`;

-- 2.1 全局事務狀態表
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

-- 2.2 分支事務狀態表
CREATE TABLE IF NOT EXISTS `branch_table` (
  `branch_id` BIGINT NOT NULL COMMENT '分支事務ID',
  `xid` VARCHAR(128) NOT NULL COMMENT '全局事務ID',
  `transaction_id` BIGINT COMMENT '事務ID',
  `resource_group_id` VARCHAR(32) COMMENT '資源分組ID',
  `resource_id` VARCHAR(256) COMMENT '資源ID',
  `branch_type` VARCHAR(8) COMMENT '分支類型 (AT, TCC, SAGA, XA)',
  `status` TINYINT COMMENT '分支事務狀態',
  `client_id` VARCHAR(64) COMMENT '客戶端唯一標識',
  `application_data` VARCHAR(2000) COMMENT '應用數據',
  `gmt_create` DATETIME(6) COMMENT '創建時間',
  `gmt_modified` DATETIME(6) COMMENT '更新時間',
  PRIMARY KEY (`branch_id`),
  KEY `idx_xid` (`xid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '分支事務狀態表記錄';

-- 2.3 全局行鎖表
CREATE TABLE IF NOT EXISTS `lock_table` (
  `row_key` VARCHAR(128) NOT NULL COMMENT '行鎖標識 (resource_id^^^table_name^^^pk)',
  `xid` VARCHAR(128) COMMENT '持有該鎖的全局事務ID',
  `transaction_id` BIGINT COMMENT '事務ID',
  `branch_id` BIGINT NOT NULL COMMENT '分支事務ID',
  `resource_id` VARCHAR(256) COMMENT '資源ID',
  `table_name` VARCHAR(32) COMMENT '表名',
  `pk` VARCHAR(36) COMMENT '主鍵值',
  `status` TINYINT NOT NULL DEFAULT '0' COMMENT '狀態',
  `gmt_create` DATETIME COMMENT '創建時間',
  `gmt_modified` DATETIME COMMENT '更新時間',
  PRIMARY KEY (`row_key`),
  KEY `idx_status` (`status`),
  KEY `idx_branch_id` (`branch_id`),
  KEY `idx_xid` (`xid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '全局行鎖表記錄';

-- 2.4 分布式排他鎖表 (Leader 選舉 / 排程任務)
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

-- ========================================================
-- 3. 初始化 seata_order (訂單微服務庫)
-- ========================================================
USE `seata_order`;

CREATE TABLE IF NOT EXISTS `t_order` (
  `id` BIGINT NOT NULL COMMENT '訂單ID (雪花算法生成)',
  `user_id` BIGINT NOT NULL COMMENT '用戶ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `count` INT NOT NULL COMMENT '數量',
  `amount` DECIMAL(10, 2) NOT NULL COMMENT '訂單金額',
  `status` VARCHAR(20) NOT NULL COMMENT '訂單狀態 (INIT, SUCCESS, FAILED)',
  `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
  `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '訂單表';

CREATE TABLE IF NOT EXISTS `undo_log` (
  `branch_id` BIGINT NOT NULL COMMENT '分支事務ID',
  `xid` VARCHAR(128) NOT NULL COMMENT '全局事務ID',
  `context` VARCHAR(128) NOT NULL COMMENT '上下文資訊',
  `rollback_info` LONGBLOB NOT NULL COMMENT '回滾信息',
  `log_status` INT NOT NULL COMMENT '狀態 (0: 正常, 1: 防懸掛已回滾)',
  `log_created` DATETIME(6) NOT NULL COMMENT '創建時間',
  `log_modified` DATETIME(6) NOT NULL COMMENT '修改時間',
  UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`),
  INDEX `ix_log_created` (`log_created`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Seata AT模式事務復原日誌表';

-- ========================================================
-- 4. 初始化 seata_storage (庫存微服務庫)
-- ========================================================
USE `seata_storage`;

CREATE TABLE IF NOT EXISTS `t_storage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主鍵ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `total` INT NOT NULL DEFAULT '0' COMMENT '總庫存',
  `used` INT NOT NULL DEFAULT '0' COMMENT '已用庫存',
  `residue` INT NOT NULL DEFAULT '0' COMMENT '剩餘庫存',
  `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_product_id` (`product_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '庫存表';

-- 初始化一筆測試庫存數據 (商品ID: 20001, 庫存 100 件)
INSERT IGNORE INTO `t_storage` (`id`, `product_id`, `total`, `used`, `residue`) 
VALUES (1, 20001, 100, 0, 100);

CREATE TABLE IF NOT EXISTS `undo_log` (
  `branch_id` BIGINT NOT NULL COMMENT '分支事務ID',
  `xid` VARCHAR(128) NOT NULL COMMENT '全局事務ID',
  `context` VARCHAR(128) NOT NULL COMMENT '上下文資訊',
  `rollback_info` LONGBLOB NOT NULL COMMENT '回滾信息',
  `log_status` INT NOT NULL COMMENT '狀態 (0: 正常, 1: 防懸掛已回滾)',
  `log_created` DATETIME(6) NOT NULL COMMENT '創建時間',
  `log_modified` DATETIME(6) NOT NULL COMMENT '修改時間',
  UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`),
  INDEX `ix_log_created` (`log_created`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Seata AT模式事務復原日誌表';

-- ========================================================
-- 5. 初始化 seata_account (帳戶錢包微服務庫 - TCC 模式)
-- ========================================================
USE `seata_account`;

CREATE TABLE IF NOT EXISTS `t_account` (
  `user_id` BIGINT NOT NULL COMMENT '用戶ID',
  `balance` DECIMAL(10, 2) NOT NULL DEFAULT '0.00' COMMENT '可用餘額',
  `freeze_amount` DECIMAL(10, 2) NOT NULL DEFAULT '0.00' COMMENT 'TCC 凍結金額',
  `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '帳戶資產表';

-- 初始化一筆測試用戶帳戶 (用戶ID: 10001, 可用餘額: 10000.00)
INSERT IGNORE INTO `t_account` (`user_id`, `balance`, `freeze_amount`) 
VALUES (10001, 10000.00, 0.00);

-- TCC 控制記錄表 (防空回滾、防業務懸掛、二階段冪等性)
CREATE TABLE IF NOT EXISTS `t_tcc_transaction_record` (
  `xid` VARCHAR(128) NOT NULL COMMENT '全局事務ID',
  `branch_id` BIGINT NOT NULL COMMENT '分支事務ID',
  `user_id` BIGINT NULL COMMENT '用戶ID',
  `status` VARCHAR(20) NOT NULL COMMENT '狀態 (TRIED, CONFIRMED, CANCELED)',
  `freeze_amount` DECIMAL(10, 2) NOT NULL DEFAULT '0.00' COMMENT '凍結金額',
  `gmt_create` DATETIME NOT NULL COMMENT '創建時間',
  PRIMARY KEY (`xid`, `branch_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'TCC 控制狀態表';

CREATE TABLE IF NOT EXISTS `undo_log` (
  `branch_id` BIGINT NOT NULL COMMENT '分支事務ID',
  `xid` VARCHAR(128) NOT NULL COMMENT '全局事務ID',
  `context` VARCHAR(128) NOT NULL COMMENT '上下文資訊',
  `rollback_info` LONGBLOB NOT NULL COMMENT '回滾信息',
  `log_status` INT NOT NULL COMMENT '狀態 (0: 正常, 1: 防懸掛已回滾)',
  `log_created` DATETIME(6) NOT NULL COMMENT '創建時間',
  `log_modified` DATETIME(6) NOT NULL COMMENT '修改時間',
  UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`),
  INDEX `ix_log_created` (`log_created`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Seata AT模式事務復原日誌表';
