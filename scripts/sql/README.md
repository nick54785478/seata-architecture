# 資料庫腳本使用手冊 (Database Scripts Guide)

本目錄存放 **Apache Seata** 相關的資料庫結構與初始化腳本。為了滿足不同角色（維運人員、業務開發者、本地測試者）的架構職責分離需求，此處維護了三份特定用途的 SQL 腳本。

---

## 腳本清單與職責矩陣

| 腳本名稱 | 面向對象 | 目標資料庫 | 核心作用與定位 |
| :--- | :--- | :--- | :--- |
| **`00_init_databases.sql`** | **本地開發 / Docker 測試** | 全部庫 (`seata_server`, `seata_order`, `seata_storage`, `seata_account`) | **【一鍵式全量腳本】**：一次建立所有資料庫、Seata TC 狀態表、各微服務業務表與測試初始數據，供 `docker-compose` 快速拉起全套環境。 |
| **`seata_server_db.sql`** | **維運 (DevOps) / DBA** | `seata_server` | **【TC 中間件專用腳本】**：僅包含 Seata Server 事務協調器運行所需的 4 張核心狀態表，與微服務業務庫完全解耦。 |
| **`undo_log.sql`** | **微服務業務開發者** | 各自的業務庫 (如 `seata_order`, `seata_storage`) | **【業務接入標準模板】**：任何微服務需要接入 Seata AT 模式時，直接拿此腳本到該業務庫執行，用以記錄前後鏡像快照。 |

---

## 一、 `00_init_databases.sql` (本地開發一鍵初始化)

### 適用場景
* 本地全新安裝測試環境。
* 搭配根目錄的 [docker-compose.yml](file:///d:/桌面/MonoRepository/seata-architecture/docker-compose.yml) 快速拉起整套微服務與中間件基礎設施。
* 自動包含商品 20001 初始庫存（100 件）與用戶 10001 初始資產（10,000 元）。

### 執行方式

#### 1. Windows PowerShell
```powershell
Get-Content 00_init_databases.sql -Raw -Encoding UTF8 | docker exec -i seata-mysql mysql -uroot -proot_password
```

#### 2. Linux / macOS Bash
```bash
docker exec -i seata-mysql mysql -uroot -proot_password < 00_init_databases.sql
```

#### 3. DBeaver / Navicat
直接開啟連線 `localhost:3307`，開啟此檔案全選執行即可。

---

## 二、 `seata_server_db.sql` (Seata TC 中間件專用)

### 適用場景
* 獨立維運部署 Seata TC Server 中間件叢集時（例如部署在 `D:\桌面\中間件\seata` 或生產環境獨立 RDS）。
* 此腳本只會建立 `seata_server` 資料庫及其專屬的 4 張內部狀態表：
  1. **`global_table`**：維護全局事務狀態機（XID、超時時間、全局狀態）。
  2. **`branch_table`**：記錄各微服務分支事務狀態（Branch ID、資源 ID）。
  3. **`lock_table`**：AT 模式全域行鎖表（防止不同事務併發修改同一筆數據造成髒寫）。
  4. **`distributed_lock`**：TC 叢集分散式排他鎖（用於定時清理任務、重試任務與 Leader 選舉）。

### 執行方式

#### 1. Windows PowerShell
```powershell
Get-Content seata_server_db.sql -Raw -Encoding UTF8 | docker exec -i seata-mysql mysql -uroot -proot_password
```

#### 2. Linux / macOS Bash
```bash
docker exec -i seata-mysql mysql -uroot -proot_password < seata_server_db.sql
```

#### 3. 驗證是否成功
```powershell
docker exec -i seata-mysql mysql -uroot -proot_password seata_server -e "SHOW TABLES;"
```
> 輸出必須包含：`branch_table`、`distributed_lock`、`global_table`、`lock_table`。

---

## 三、 `undo_log.sql` (業務微服務 AT 模式標準模板)

### 適用場景
* 未來專案擴充新微服務時（例如新開發了 `coupon-service` 或 `delivery-service`）。
* 只要該微服務的持久層打算採用 **Seata AT 模式**，就必須在其專屬的業務資料庫中執行此腳本。
* **作用原理**：Seata 的 `DataSourceProxy` 會在執行業務 SQL 前後查詢數據鏡像，將序列化後的二進位快照存入此表的 `rollback_info` 欄位中，以供二階段異常時自動進行反向補償還原。

### 執行方式
進入具體的業務資料庫執行即可，例如：
```sql
USE your_business_database;

-- 執行 undo_log 建立語句
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
```

---

## 四、 常見注意事項

1. **PowerShell 重定向問題**：
   在 Windows PowerShell 下請避免使用 `< file.sql`（`<` 為保留字元），請一律使用 `Get-Content file.sql -Raw -Encoding UTF8 | docker exec ...`。
2. **字符集編碼**：
   所有表均採用 `utf8mb4` 字符集與 `utf8mb4_unicode_ci` 排序規則，確保完整相容繁體中文與各類特殊符號。
3. **主鍵與索引**：
   AT 模式要求業務表**必須具備明確的主鍵（Primary Key）**，否則 Seata 無法鎖定行記錄與生成前後鏡像。
