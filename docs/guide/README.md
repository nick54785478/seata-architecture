# Seata 微服務分散式事務架構實戰使用指南

> 本指南詳細介紹本微服務系統的**環境準備**、**中間件啟動**、**微服務編譯與運行**、**單元測試驗證**，以及**真實端到端 (E2E) 下單與分散式回滾測試流程**。

---

## 目錄

- [一、 系統架構與服務清單](#一-系統架構與服務清單)
- [二、 前置環境要求](#二-前置環境要求)
- [三、 基礎中間件與資料庫啟動](#三-基礎中間件與資料庫啟動)
- [四、 本地單元測試驗證 (BDD Gherkin)](#四-本地單元測試驗證-bdd-gherkin)
- [五、 微服務啟動指南](#五-微服務啟動指南)
- [六、 端到端 (E2E) 實戰情境驗證](#六-端到端-e2e-實戰情境驗證)
  - [情境 1：正常下單流程 (二階段提交 Commit)](#情境-1正常下單流程-二階段提交-commit)
  - [情境 2：人為模擬異常 (二階段分散式回滾 Rollback)](#情境-2人為模擬異常-二階段分散式回滾-rollback)
  - [情境 3：庫存或餘額不足防禦測試](#情境-3庫存或餘額不足防禦測試)
- [七、 常見問題排查 (Troubleshooting FAQ)](#七-常見問題排查-troubleshooting-faq)

---

## 一、 系統架構與服務清單

本系統實作了標準的**「下單 - 扣庫存 - 錢包扣款」**全鏈路交易，並融合了 **AT 模式（無侵入自動補償）** 與 **TCC 模式（顯式資金預留防懸掛）**：

| 微服務模組 | 監聽 Port | 數據庫 | 採用的 Seata 技術 | 核心職責 |
| :--- | :--- | :--- | :--- | :--- |
| **`gateway-service`** | `8080` | 無 | 路由網關 (不參與事務) | 統一外部流量入口、動態負載均衡。 |
| **`order-service`** | `8081` | `seata_order` | **TM** + **RM (AT 模式)** | 全局事務發起者（`@GlobalTransactional`），持久化訂單實體。 |
| **`storage-service`** | `8082` | `seata_storage` | **RM (AT 模式)** | 商品庫存扣減，由 DataSourceProxy 自動捕獲前後鏡像並生成 `undo_log`。 |
| **`account-service`** | `8083` | `seata_account` | **RM (TCC 模式)** | 金融資金結算，提供 Try(凍結)、Confirm(劃扣)、Cancel(解凍) 三階段。 |
| **`shared-module`** | - | - | 基礎通用庫 | 提供共用 DTO、統一 Result 封裝、TCC 狀態常數。（詳見 [shared-module 跨專案引入指南](file:///d:/桌面/MonoRepository/seata-architecture/docs/guide/02-shared-module-dependency-guide.md)） |

---

## 二、 前置環境要求

* **JDK**：Java 21 或 Java 22（推薦 Eclipse Temurin 21+）
* **構建工具**：Apache Maven 3.9+
* **容器環境**：Docker & Docker Compose (v2.x+)
* **測試工具**：cURL、Postman 或 HTTP 測試客戶端

---

## 三、 基礎中間件與資料庫啟動

在執行微服務前，需確保 Nacos、MySQL、Seata Server 與 Redis 正常運行。

### 1. 啟動中間件容器
根目錄提供了已修復所有驅動與參數的 [docker-compose.yml](file:///d:/桌面/MonoRepository/seata-architecture/docker-compose.yml)：

```bash
# 啟動 MySQL, Redis, Nacos 與 Seata TC Server
docker-compose up -d

# 檢查容器狀態 (確保均為 running / healthy)
docker-compose ps
```

### 2. 初始化資料庫與測試資料
若為全新啟動的 MySQL 容器，請執行 [00_init_databases.sql](file:///d:/桌面/MonoRepository/seata-architecture/scripts/sql/00_init_databases.sql) 腳本一次建立所有資料庫、Seata 狀態表與初始測試數據：

* **Windows PowerShell**：
  ```powershell
  Get-Content scripts/sql/00_init_databases.sql -Raw -Encoding UTF8 | docker exec -i seata-mysql mysql -uroot -proot_password
  ```
* **Linux / macOS Bash**：
  ```bash
  docker exec -i seata-mysql mysql -uroot -proot_password < scripts/sql/00_init_databases.sql
  ```

### 3. 驗證控制台存取
* **Nacos 後台**：`http://localhost:8848/nacos`（帳號 / 密碼：`nacos` / `nacos`）
* **Seata TC 控制台**：`http://localhost:7091`（帳號 / 密碼：`seata` / `seata`）
* **Seata RPC 埠**：`localhost:8091`（已正常監聽）

---

## 四、 本地單元測試驗證 (BDD Gherkin)

本專案完全遵循 `AGENTS.md` 開發規範，領域層採純 Java 隔離，每個 UseCase 均具備 BDD Gherkin 語法描述的單元測試，**不需要依賴外部中間件即可離線驗證核心業務邏輯**。

### 執行單元測試指令：
```powershell
mvn test -Dtest=*UseCaseTest -Dsurefire.failIfNoSpecifiedTests=false
```

### 核心測試案例清單：
1. **`DeductStorageUseCaseTest` (庫存服務)**：
   * `givenSufficientStock_whenDeduct_thenSuccess`：庫存充足時扣減成功，驗證領域聚合根狀態與領域事件發布。
   * `givenInsufficientStock_whenDeduct_thenThrowException`：庫存不足時拋出領域例外 `StockInsufficientException`。
2. **`PrepareAccountPaymentUseCaseTest` (帳戶服務 - TCC)**：
   * `givenSufficientBalance_whenPrepare_thenSuccess`：TCC Try 成功凍結資金，可用餘額減少，狀態記錄為 `TRIED`。
   * `givenCanceledRecord_whenPrepare_thenPreventSuspension`：**防業務懸掛驗證**，當檢測到已存在 `CANCELED` 記錄時拒絕 Try 執行。
   * `givenInsufficientBalance_whenPrepare_thenThrowException`：餘額不足時拋出領域例外 `BalanceInsufficientException`。
3. **`CreateOrderUseCaseTest` (訂單服務 - TM)**：
   * `givenValidOrder_whenCreateOrder_thenSuccess`：正常下單，全鏈路調用庫存與帳戶服務並持久化訂單。
   * `givenSimulateError_whenCreateOrder_thenThrowException`：模擬異常觸發全局回滾機制。

---

## 五、 微服務啟動指南

### 建議啟動順序
為避免下游服務未就緒造成 Feign 調用連線異常，請依序啟動微服務：

1. **第 1 步：啟動庫存服務 (`storage-service`)**
   * 進入 `storage-service`，啟動 `StorageApplication.java`（監聽 `8082` 埠）。
2. **第 2 步：啟動帳戶服務 (`account-service`)**
   * 進入 `account-service`，啟動 `AccountApplication.java`（監聽 `8083` 埠）。
3. **第 3 步：啟動訂單服務 (`order-service`)**
   * 進入 `order-service`，啟動 `OrderApplication.java`（監聽 `8081` 埠）。
4. **第 4 步 (可選)：啟動網關服務 (`gateway-service`)**
   * 進入 `gateway-service`，啟動 `GatewayApplication.java`（監聽 `8080` 埠）。

> **確認註冊狀態**：啟動後登入 Nacos 控制台，確認「服務列表」中出現 `order-service`、`storage-service`、`account-service`。

---

## 六、 端到端 (E2E) 實戰情境驗證

> 📖 **完整實戰測試手冊**：關於各情境的詳細時序架構圖、完整 Request / Response Payload、數據庫前後對比 SQL 與邊界防禦測試，請直接查閱專門手冊：
> 👉 **[03-end-to-end-verification-guide.md (全鏈路端到端實戰測試指南)](file:///d:/桌面/MonoRepository/seata-architecture/docs/guide/03-end-to-end-verification-guide.md)**

### 初始測試數據基準
由 `00_init_databases.sql` 預設寫入的數據：
* 商品 ID **`20001`**：總庫存 100 件，已用 0 件，**剩餘 100 件**。
* 用戶 ID **`10001`**：可用餘額 **10,000.00 元**，凍結金額 0.00 元。

---

### 情境 1：正常下單流程 (二階段提交 Commit)

#### 1. 發送請求
購買商品 20001 共 2 件，支付金額 500.00 元，`simulateError: false`：

```bash
curl -X POST http://localhost:8080/api/order/create \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "productId": 20001,
    "count": 2,
    "payAmount": 500.00,
    "simulateError": false
  }'
```

#### 2. 預期 API 回應
```json
{
  "code": 200,
  "message": "SUCCESS",
  "data": {
    "orderId": 17322384729104,
    "userId": 10001,
    "productId": 20001,
    "count": 2,
    "amount": 500.00,
    "status": "SUCCESS"
  }
}
```

#### 3. 資料庫狀態驗證 (連線埠 3307)
* **庫存庫 (`seata_storage`)**：
  ```sql
  SELECT product_id, total, used, residue FROM seata_storage.t_storage WHERE product_id = 20001;
  -- 結果：total=100, used=2, residue=98
  ```
* **帳戶庫 (`seata_account`)**：
  ```sql
  SELECT user_id, balance, freeze_amount FROM seata_account.t_account WHERE user_id = 10001;
  -- 結果：balance=9500.00, freeze_amount=0.00 (TCC Confirm 已正式劃扣)
  ```
* **訂單庫 (`seata_order`)**：
  ```sql
  SELECT id, user_id, product_id, count, amount, status FROM seata_order.t_order;
  -- 結果：存在該筆訂單，status='SUCCESS'
  ```
* **復原日誌 (`undo_log`)**：
  ```sql
  SELECT * FROM seata_storage.undo_log;
  -- 結果：二階段提交後已被非同步自動清理 (清空)
  ```

---

### 情境 2：人為模擬異常 (二階段分散式回滾 Rollback)

這是驗證 Seata 分散式事務核心價值的關鍵場景。下單請求中傳入 `"simulateError": true`，訂單服務在調用完庫存與帳戶扣減後，故意在本地拋出異常。

#### 1. 發送請求
```bash
curl -X POST http://localhost:8080/api/order/create \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "productId": 20001,
    "count": 2,
    "payAmount": 500.00,
    "simulateError": true
  }'
```

#### 2. 預期 API 回應
* HTTP 500 異常：`【模擬業務異常】觸發 Seata 全局分散式事務回滾！`

#### 3. 神奇的自動補償驗證 (關鍵檢查點！)
* **檢查庫存庫**：
  ```sql
  SELECT product_id, total, used, residue FROM seata_storage.t_storage WHERE product_id = 20001;
  -- 結果：residue 依然是 98 (沒有被扣除成 96！Seata AT 模式透過 undo_log 自動逆向補償恢復！)
  ```
* **檢查帳戶庫**：
  ```sql
  SELECT user_id, balance, freeze_amount FROM seata_account.t_account WHERE user_id = 10001;
  -- 結果：balance 依然是 9500.00，freeze_amount=0.00 (Seata TC 自動調用二階段 Cancel 解凍退回款項！)
  ```
* **檢查訂單庫**：
  ```sql
  SELECT * FROM seata_order.t_order;
  -- 結果：沒有殘留該次異常失敗的訂單髒資料
  ```

---

### 情境 3：庫存或餘額不足防禦測試

#### 1. 餘額不足防禦
傳入超額支付金額（例如 `payAmount: 9999999.00`）：
```bash
curl -X POST http://localhost:8080/api/order/create \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "productId": 20001,
    "count": 1,
    "payAmount": 9999999.00,
    "simulateError": false
  }'
```
* **預期行為**：`account-service` 拋出 `BalanceInsufficientException`，一階段 Try 失敗中斷鏈路；已扣減庫存的分支事務自動回滾，庫存不變。

---

## 七、 常見問題排查 (Troubleshooting FAQ)

### Q1: Seata TC Server 啟動時報 `store.db.dbType should not be blank`？
* **原因**：Seata 2.x 採用 DB 儲存模式時未指明資料庫類型。
* **排查**：請檢查 `docker-compose.yml` 中的 `seata-server` 是否包含環境變量 `STORE_DB_DB_TYPE=mysql` 與 JVM 參數 `-Dstore.db.dbType=mysql`。

### Q2: 啟動時報 `the {com.mysql.jdbc.Driver} can't be found in the path /lib/jdbc/`？
* **原因**：Apache 官方發行版映像檔（`apache/seata-server`）受限於 Apache 基金會開源協議，未內置 GPL 授權的 MySQL JDBC 驅動。
* **解決辦法**：在 `docker-compose.yml` 中直接使用已內建 MySQL 驅動的社群完整鏡像 **`seataio/seata-server:1.8.0`**。

### Q3: 啟動時報 `Table 'seata_server.distributed_lock' doesn't exist`？
* **原因**：MySQL 容器內的 `seata_server` 資料庫尚未初始化表結構。
* **解決辦法**：請依照 [scripts/sql/README.md](file:///d:/桌面/MonoRepository/seata-architecture/scripts/sql/README.md) 執行 `00_init_databases.sql` 導入 Seata TC 所需的 4 張內部狀態表。

### Q4: 為什麼微服務更新資料庫後，Seata AT 模式沒有攔截到 SQL？
* **原因**：Spring Data JPA (Hibernate) 預設為延遲提交 (Write-Behind)，若在方法結束前未發送 SQL，Seata 的 `DataSourceProxy` 無法及時截獲。
* **解決辦法**：本專案在 `StorageRepositoryAdapter` 與 `OrderRepositoryAdapter` 中均強制使用 **`saveAndFlush()`**，確保即時刷新至 JDBC 連線層。
