# 03. AT 模式核心機制深度解構

AT 模式是 Seata 中應用最廣、設計最巧妙的模式。本文深入剖析其底層 SQL 解析、Undo Log 生成、全局行鎖與隔離級別防護。

---

## 1. AT 模式底層詳細執行流程

以 SQL：`UPDATE account SET money = money - 100 WHERE id = 1;` 為例：

```mermaid
flowchart TD
    Start([業務執行業務 SQL]) --> Parse[1. SQL 解析器解析語法樹]
    Parse --> QueryBefore[2. 構建 SELECT 語句查詢 Before Image]
    QueryBefore --> SaveBefore[儲存更新前狀態: money = 500]
    SaveBefore --> ExecSQL[3. 執行業務 UPDATE SQL]
    ExecSQL --> QueryAfter[4. 構建 SELECT 語句查詢 After Image]
    QueryAfter --> SaveAfter[儲存更新後狀態: money = 400]
    SaveAfter --> BuildUndo[5. 組裝 UndoLog 資訊 (前後鏡像快照)]
    BuildUndo --> ReqLock[6. 向 TC 申請全局行鎖 (account:1)]
    ReqLock --> CheckLock{鎖是否獲取成功?}
    CheckLock -- 否 (重試超時) --> RollbackLocal[回滾本地事務，拋出異常]
    CheckLock -- 是 --> InsertUndo[7. 插入 undo_log 表]
    InsertUndo --> CommitLocal[8. 提交本地事務 (業務更新 + UndoLog 同時提交)]
    CommitLocal --> ReportTC[9. 匯報分支事務執行成功至 TC]
```

---

## 2. Undo Log 核心結構剖析

Seata 客戶端會在業務庫中自動維護 `undo_log` 表：

```sql
CREATE TABLE `undo_log` (
  `branch_id` BIGINT NOT NULL COMMENT '分支事務ID',
  `xid` VARCHAR(128) NOT NULL COMMENT '全局事務ID',
  `context` VARCHAR(128) NOT NULL COMMENT '上下文',
  `rollback_info` LONGBLOB NOT NULL COMMENT '前後鏡像序列化二進制 (JSON/Jackson/Kryo/Protostuff)',
  `log_status` INT NOT NULL COMMENT '0:正常, 1:防懸掛已回滾',
  `log_created` DATETIME(6) NOT NULL,
  `log_modified` DATETIME(6) NOT NULL,
  UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
);
```

### `rollback_info` 內部邏輯表示：
```json
{
  "tableName": "account",
  "beforeImage": {
    "rows": [
      {
        "fields": [
          {"name": "id", "type": 4, "value": 1},
          {"name": "money", "type": 3, "value": 500}
        ]
      }
    ]
  },
  "afterImage": {
    "rows": [
      {
        "fields": [
          {"name": "id", "type": 4, "value": 1},
          {"name": "money", "type": 3, "value": 400}
        ]
      }
    ]
  }
}
```

---

## 3. 全局鎖 (Global Lock) 與隔離級別

### 3.1 為什麼需要全局鎖？（防止髒寫 Dirty Write）
在 AT 模式下一階段本地事務就已經提交並釋放了本地 DB 排他鎖。如果有**未納入 Seata 全局事務管理**的外部 SQL，或另一個全局事務試圖修改同一行數據：
- 假設全局事務 A 將 `money` 從 100 改為 80 並提交本地 DB。
- 此時外部事務 B 直接將 `money` 改為 50 並提交。
- 若事務 A 二階段需要回滾，若直接覆蓋回 100，則事務 B 的更新將被意外覆蓋（丟失更新 / 髒寫）。

### 3.2 全局鎖機制
- 全局鎖由 **TC (Transaction Coordinator)** 統一維護，在 TC 的 `lock_table` 中記錄：
  `row_key = resource_id + "^^^" + table_name + "^^^" + pk_value`
- 只有持有全局鎖的事務，才能提交本地事務。如果未搶到全局鎖，RM 會進行自旋重試（預設重試間隔 10ms，最大重試 30 次）。

### 3.3 髒寫檢測與回滾策略
當 TC 通知 RM 執行二階段回滾時：
1. RM 比對資料庫當前最新數據與 **After Image**：
   - **無髒寫**：當前數據 == After Image，說明中間沒有其他外部操作竄改數據。RM 生成反向補償 SQL（`UPDATE account SET money = 500 WHERE id = 1`），執行並提交，刪除 `undo_log`。
   - **發生髒寫**：當前數據 != After Image，說明該行數據被外部操作意外修改！Seata 會暫停自動回滾，記錄告警日誌（需要人工介入介入審計）。

### 3.4 隔離級別防護：讀已提交 (Read Committed)
預設情況下，Seata AT 模式的隔離級別是**讀未提交（Read Uncommitted）**，因為一階段本地事務已提交，其他事務可讀到中間狀態。

**如何保證讀取到全局已提交的數據？**
使用 `@GlobalLock` 配合 `SELECT ... FOR UPDATE`：
```java
@GlobalLock // 告知 Seata 攔截器校驗全局鎖
@Transactional
public Account getAccountForUpdate(Long id) {
    // Seata 代理會攔截此語句，向 TC 確認該行數據是否被其他未提交的全局事務鎖定
    // 若被鎖定，等待其釋放，確保讀取到全局最終一致的已提交數據
    return accountMapper.selectByIdForUpdate(id);
}
```
