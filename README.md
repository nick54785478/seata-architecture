# Apache Seata 分散式事務架構全景與企業級落地實戰

> **本專案為企業級微服務分散式事務與高強韌性架構的實戰參考設計。以電商「下單 - 扣減庫存 (AT) - 劃扣錢包 (TCC)」跨服務交易鏈路為業務切片，聚焦於「分散式資料最終一致性治理」、「零框架侵入之純粹領域驅動設計 (DDD)」與「六角形架構 (Ports & Adapters) 業務邊界隔離」，建構出高內聚、低耦合且具備自動二階段補償能力的高可靠微服務系統。**

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.3-blue.svg)](https://spring.io/projects/spring-cloud)
[![Apache Seata](https://img.shields.io/badge/Apache%20Seata-2.0.0-red.svg)](https://seata.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

---

## 目錄導引

- [什麼是 Apache Seata 分散式事務？](#什麼是-apache-seata-分散式事務)
  - [1. 為什麼微服務需要分散式事務？](#1-為什麼微服務需要分散式事務)
  - [2. Seata 三大核心角色 (TC / TM / RM)](#2-seata-三大核心角色-tc--tm--rm)
  - [3. Seata 四大事務模式與本專案選型 (AT + TCC 混合實作)](#3-seata-四大事務模式與本專案選型-at--tcc-混合實作)
- [一、 核心架構設計理念與規範準則](#一-核心架構設計理念與規範準則)
- [二、 技術棧與中介軟體整合矩陣](#二-技術棧與中介軟體整合矩陣)
- [三、 系統全景與服務拓撲](#三-系統全景與服務拓撲)
- [四、 業務與 RPC API 快速規格表](#四-業務與-rpc-api-快速規格表)
- [五、 5 分鐘快速啟動與上手指南](#五-5-分鐘快速啟動與上手指南)
- [六、 端到端 (E2E) 核心測試情境](#六-端到端-e2e-核心測試情境)
- [七、 專案目錄與模組職責說明](#七-專案目錄與模組職責說明)
- [八、 深度架構文檔導覽 (Docs Index)](#八-深度架構文檔導覽-docs-index)

---

## 什麼是 Apache Seata 分散式事務？

### 1. 為什麼微服務需要分散式事務？

在傳統單體架構中，所有業務操作共享同一個資料庫實體，資料的一致性（ACID 特性）可直接依賴關聯式資料庫的本地事務機制。

然而在**微服務架構**下，系統依據業務邊界被拆解為多個自治服務，並實施 **Database-per-Service（每服務獨立資料庫）** 架構（例如：訂單庫 `seata_order`、庫存庫 `seata_storage`、帳戶庫 `seata_account`）。服務間透過網路 RPC（如 OpenFeign、REST API）通訊：
- **網路呼叫不確定性**：可能發生超時、丟包或節點崩潰。
- **本地事務跨庫失效**：各微服務只能保證自身的本地事務，無法感知遠端服務的執行結果。若「訂單建立成功、庫存扣減成功，但帳戶服務因餘額不足扣款失敗」，傳統架構將導致庫存超賣與嚴重的資料不一致（Data Inconsistency）。

為了解決跨微服務、跨獨立資料庫的資料最終一致性，**分散式事務 (Distributed Transaction)** 成為微服務架構不可或缺的核心基礎設施。

---

### 2. Seata 三大核心角色 (TC / TM / RM)

**Apache Seata (Simple Extensible Autonomous Transaction Architecture)** 是一套開源的高效能、易於使用的分散式事務架構，致力於在微服務體系中提供輕量、無侵入且高可靠的全局事務解決方案。

Seata 定義了三大核心角色協同完成全鏈路事務治理：
1. **TC (Transaction Coordinator - 事務協調器)**：
   - 作為獨立運行的中間件伺服器（Seata Server，本專案基於 Docker 容器執行於 `8091` / `7091`）。
   - 維護全局事務與各分支事務的執行狀態，並根據全鏈路執行結果向所有分支驅動**二階段全局提交 (Global Commit)** 或**二階段全局回滾 (Global Rollback)**。
2. **TM (Transaction Manager - 事務管理器)**：
   - 嵌入在發起全局事務的業務發起端服務（本專案由 [order-service](file:///d:/桌面/MonoRepository/seata-architecture/order-service) 擔任）。
   - 透過 `@GlobalTransactional` 註解宣告全局事務的邊界，向 TC 申請開啟全局事務（產生全局唯一 `XID`）、請求提交或發起回滾。
3. **RM (Resource Manager - 資源管理器)**：
   - 嵌入在各分支業務服務（本專案由 [storage-service](file:///d:/桌面/MonoRepository/seata-architecture/storage-service) 與 [account-service](file:///d:/桌面/MonoRepository/seata-architecture/account-service) 擔任）。
   - 負責管理各分支事務處理的資源，向 TC 註冊分支事務並回報分支狀態，並依照 TC 的指令執行具體分支的提交或回滾操作。

```mermaid
sequenceDiagram
    autonumber
    participant TM as 事務發起端 [TM]<br/>(Order Service)
    participant TC as 事務協調器 [TC]<br/>(Seata Server :8091)
    participant RM_Storage as 分支資源 [RM 1]<br/>(Storage Service [AT])
    participant RM_Account as 分支資源 [RM 2]<br/>(Account Service [TCC])

    TM->>TC: 1. 開啟全局事務 (Begin Global Tx)
    TC-->>TM: 回傳全局唯一 XID (全鏈路透過 HTTP Header 透傳)
    TM->>RM_Storage: 2. RPC 調用扣減庫存 (攜帶 TX_XID)
    RM_Storage->>TC: 3. 註冊 AT 分支事務 (寫入 undo_log)
    RM_Storage-->>TM: 庫存扣減完成

    TM->>RM_Account: 4. RPC 調用資金 Try (攜帶 TX_XID)
    RM_Account->>TC: 5. 註冊 TCC 分支事務 (凍結款項)
    RM_Account-->>TM: 資金凍結完成

    alt 業務執行順利無異常
        TM->>TC: 6a. 請求全局二階段提交 (Commit Global Tx)
        par 非同步二階段提交
            TC->>RM_Storage: 7a. AT Commit (非同步清理 undo_log)
        and
            TC->>RM_Account: 7b. TCC Confirm (正式劃扣資金，釋放凍結)
        end
    else 業務拋出異常 / 中斷 (如 simulateError 或餘額不足)
        TM->>TC: 6b. 請求全局二階段回滾 (Rollback Global Tx)
        par 非同步二階段自動補償
            TC->>RM_Storage: 7c. AT Rollback (讀取 undo_log 反向還原庫存)
        and
            TC->>RM_Account: 7d. TCC Cancel (執行解凍邏輯，將款項退回餘額)
        end
    end
```

---

### 3. Seata 四大事務模式與本專案選型 (AT + TCC 混合實作)

Seata 支援四種事務模式，涵蓋不同的業務場景與效能權衡：

| 模式 | 核心機制 | 業務侵入性 | 隔離性 | 適用場景 | 本專案實作應用 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **AT 模式** (Automatic) | 基於資料庫 `undo_log`，自動解析 SQL 截取前後鏡像 (`before-image` / `after-image`) 生成反向補償 SQL | **零侵入** | 讀已提交 (全局行鎖防髒寫) | 一般關聯式資料庫業務 (MySQL / PostgreSQL / Oracle) | **`order-service`** (本地訂單)<br/>**`storage-service`** (扣減庫存) |
| **TCC 模式** (Try-Confirm-Cancel) | 業務手動實作一階段預留資源 (Try)、二階段劃扣 (Confirm) 與解凍 (Cancel) | **較高** | 業務層面邏輯隔離 | 高並發、跨異質系統、金融核心交易、非關聯式儲存 | **`account-service`** (資金凍結劃扣) |
| **Saga 模式** | 長事務編排模式，由狀態機定義前向操作與逆向補償呼叫 | 中等 | 無隔離性 | 跨銀行、多方協作、長時間業務流程 | 參閱 [Saga 模式詳解](file:///d:/桌面/MonoRepository/seata-architecture/docs/seata/02-transaction-modes.md) |
| **XA 模式** | 基於資料庫原生 XA 規範強一致性協議 | **零侵入** | 強一致性 (持有底層資料庫鎖) | 金融級強一致性要求、低並發場景 | 參閱 [XA 模式詳解](file:///d:/桌面/MonoRepository/seata-architecture/docs/seata/02-transaction-modes.md) |

💡 **本專案的核心實戰價值**：
企業在面對大型微服務架構時，往往需要兼顧**開發效率（庫存與訂單業務量大，優先採用無侵入的 AT 模式）**與**核心資產交易的高並發安全（錢包劃扣採用 TCC 模式，手動控制資源凍結，徹底規避資料庫全局行鎖競爭並解決懸掛與空回滾問題）**。本專案即為企業級 **AT + TCC 混合雙模式** 的最佳實踐範例。

---

## 一、 核心架構設計理念與規範準則 (Architectural Principles)

本專案遵循嚴格的架構治理與企業級設計規範（詳見 [.agents/AGENTS.md](file:///d:/桌面/MonoRepository/seata-architecture/.agents/AGENTS.md)），將分散式交易機制與純粹的領域模型完全解耦：

### 1. 跨微服務資料最終一致性架構 (AT + TCC 混合雙模式協同)
- **業務切片定位**：採用「下單 (Order) - 扣減庫存 (Storage) - 劃扣錢包 (Account)」經典電商主鏈路，全面覆蓋分散式系統中最核心的長交易與資產安全訴求。
- **事務管理器 (TM) 邊界治理**：由入口層 `order-service` 擔任 TM，以 `@GlobalTransactional` 定義全局事務邊界，向 TC 申請並獲取全局唯一事務識別碼 (`XID`)，並透過 HTTP Header (`TX_XID`) 跨 RPC 鏈路無縫傳播。
- **資源管理器 (RM) 模式互補選型**：
  - **庫存微服務 (Storage Service - AT 模式)**：對外開放無侵入扣減介面，由 Seata DataSourceProxy 自動截取 SQL 執行前後鏡像 (`before-image` / `after-image`) 並持久化至 `undo_log`，二階段由 TC 自動生成逆向補償 SQL，兼顧極高開發效率與可靠性。
  - **帳戶微服務 (Account Service - TCC 模式)**：針對高並發、高靈敏度的資金交易，採用手動預留模型。一階段 Try 凍結可用餘額，二階段 Confirm 劃扣或 Cancel 解凍。內建事務控制表記錄 (`t_tcc_transaction_record`)，有效解決**空回滾 (Empty Rollback)**、**業務懸掛 (Suspension)** 與**重複回報冪等性 (Idempotency)**。

### 2. 純粹領域驅動設計 (Domain-Driven Design, DDD)
- **核心領域至上 (Core Domain First)**：商業規則、狀態變更不變性 (Invariants) 完全內聚於 `domain/` 聚合根中，對外完全隱蔽內部細節。
- **技術無關性 (Framework Agnostic)**：`domain/` 層內部嚴格維持 **Pure Java**，嚴禁依賴任何技術框架（如 Spring 註解、JPA ORM 實體、Jackson 序列化註解等），確保領域模型不受外部基礎設施演進所污染。
- **嚴格禁止 Lombok 破壞聚合根封裝**：Aggregate Root 內部**嚴禁使用 Lombok**（如 `@Data`, `@Getter`, `@Setter`），所有狀態轉換必須由具備明確業務語意的行為方法觸發（例如 `storage.deduct(count)`、`account.freeze(amount)`），確保業務封裝邊界的完整性。
- **垂直切片架構 (Vertical Slice Architecture)**：Domain Layer 依據 Aggregate 聚合邊界切片，專屬的 `aggregate/` (Root, Entity, VO)、`event/` (Domain Events) 與 `exception/` (領域專屬例外) 緊密內聚於該聚合目錄下。

### 3. 六角形架構 (Hexagonal / Ports & Adapters Architecture) 與防腐機制
- **Inbound Port (應用契約)**：所有對外業務開放能力一律宣告於 `port/in/`，並以 `UseCase` 命名稱謂（如 `CreateOrderUseCase`、`GetStorageUseCase`），純粹接收 Command/Query 並返回 DTO/Result。
- **Outbound Port (基礎設施契約)**：所有對外部系統（資料庫、中介軟體、RPC）的需求一律宣告於 `port/out/`，並以 `Port` 命名稱謂（如 `OrderRepositoryPort`、`AccountClientPort`）。
- **防腐層 (Anti-Corruption Layer, ACL)**：表現層 Request/Response (`Resource`) 與應用層 DTO/Command 嚴守「純資料載體 (Pure Data Carrier)」職責，嚴禁於資料對象內部撰寫轉換邏輯；所有物件轉換一律交由獨立的 `assembler/` 轉換器承擔。

### 4. 嚴格之物件命名與職責分工 (9 大命名規範)
專案內所有物件命名必須嚴格遵循下表，確保代碼具備自我解釋能力與架構一致性：
| 類別 | 命名規範 | 架構職責說明 | 範例 |
| :--- | :--- | :--- | :--- |
| **Request** | `V + N + Resource` | 表現層對外接收的請求載體 | `CreateOrderResource`, `DeductStorageResource` |
| **Response** | `N + Ved + Resource` | 表現層對外回傳的回應載體 | `OrderCreatedResource`, `StorageRetrievedResource` |
| **Event** | `N + Ved + Event` | 領域狀態變更時發布的領域事件 | `OrderCreatedEvent`, `StockDeductedEvent` |
| **Command** | `V + N + Command` | 應用層寫入或變更狀態之命令載體 | `CreateOrderCommand`, `DeductStorageCommand` |
| **Query** | `V + N + Query` | 應用層只讀查詢條件之載體 | `GetOrderQuery`, `GetStorageQuery` |
| **DTO** | `N + Gotten/Searched + Result` | 應用層查詢與業務處理結果傳輸物件 | `OrderGottenResult`, `StorageGottenResult` |
| **Projection** | `N + Gotten/Searched + View` | 應用層特定視圖投影物件 | `OrderGottenView` |
| **Port** | `in: ... + UseCase`<br>`out: ... + Port` | 六角形架構之輸入業務契約與輸出外部合約 | `CreateOrderUseCase`, `StorageRepositoryPort` |
| **Adapter** | `in: ... + ApplicationService`<br>`out: ... + Adapter` | 實作 Inbound UseCase 服務與 Outbound Port 適配器 | `OrderApplicationService`, `StorageRepositoryAdapter` |

### 5. 全鏈路可觀測性與即時業務查詢架構
- **業務語意查詢 API**：各微服務針對其領域實體暴露以業務識別碼為依據的查詢端點（`GET /api/storage/{productId}`、`GET /api/account/{userId}`、`GET /api/order/{orderId}`），使 E2E 驗證與運維排查完全透明化。
- **自動基準數據校準架構**：微服務內建 `DataInitializationConfig`，於 Application 啟動階段自動校驗並修復測試基準數據，重啟即自動恢復至標準測試狀態。

### 6. 分散式雪花演算法 (Snowflake ID) 與主鍵策略
- **解決 JPA 延遲 Flush 與批處理衝突**：避免使用資料庫自增（`IDENTITY`）迫使 JPA 提前發送單條 `INSERT` 打斷批處理並打亂 Seata 截獲時機。
- **純粹演算法實作 (`shared-module`)**：實作標準 64-bit [SnowflakeIdGenerator](file:///d:/桌面/MonoRepository/seata-architecture/shared-module/src/main/java/com/seata/demo/shared/util/SnowflakeIdGenerator.java)，包含 41-bit 時間戳、5-bit 資料中心、5-bit 工作節點與 12-bit 序列號，具備 5ms 內時鐘抖動自旋保護。
- **六角形契約解耦 (`order-service`)**：應用層透過 [IdGeneratorPort](file:///d:/桌面/MonoRepository/seata-architecture/order-service/src/main/java/com/seata/demo/order/application/port/out/IdGeneratorPort.java) 宣告主鍵需求，由基礎設施層 [SnowflakeIdGeneratorAdapter](file:///d:/桌面/MonoRepository/seata-architecture/order-service/src/main/java/com/seata/demo/order/infrastructure/adapter/SnowflakeIdGeneratorAdapter.java) 實作，使聚合根實體在持久化前即具備明確業務主鍵，完全保持 DDD 領域模型之純粹性。

---

## 二、 技術棧與中介軟體整合矩陣 (Technology Stack & Infrastructure)

本專案將所有框架、技術相依與中介軟體獨立於業務領域之外，集中配置與組裝於基礎設施層：

| 維度分類 | 技術組件 / 中介軟體 | 版本選型 | 在架構中的職責與應用 |
| :--- | :--- | :--- | :--- |
| **核心語言** | **Java (OpenJDK)** | `21 (LTS)` | 基礎運行環境，利用虛擬執行緒 (Virtual Threads)、Record 與 Pattern Matching 特性提升高並發處理能力 |
| **微服務底座** | **Spring Boot** | `3.3.4` | 微服務基座、自動配置引擎與技術組裝層 (The Dirtiest Layer) |
| **雲原生微服務** | **Spring Cloud** | `2023.0.3` | 微服務全鏈路生態基礎架構 |
| **微服務治理** | **Spring Cloud Alibaba** | `2023.0.1.2` | 整合 Alibaba 開源微服務生態（Nacos & Seata 核心適配） |
| **分散式事務協調** | **Apache Seata** | `2.0.0` | 核心分散式事務解決方案：<br/>• **Seata Server (TC)**：Netty RPC (`8091`) 狀態管理與二階段協調<br/>• **Seata Client (TM/RM)**：AOP 事務切面、DataSourceProxy 與 TCC 攔截器 |
| **服務註冊與配置** | **Alibaba Nacos** | `2.3.2` | 微服務註冊發現、健康檢查、叢集路由動態推播與集中化配置中心 (`8848`) |
| **API 網關** | **Spring Cloud Gateway** | `4.1.5` | 統一微服務訪問入口 (`8080`)，負責基於路徑的動態路由轉發、全局 CORS 跨域處理與請求限流 |
| **聲明式 RPC 呼叫** | **Spring Cloud OpenFeign** | `4.1.3` | 跨微服務 HTTP 呼叫，客製化 `RequestInterceptor` 自動提取並透傳 `TX_XID` 全局事務上下文 |
| **持久化 ORM 引擎** | **Spring Data JPA / Hibernate** | `3.3.4` / `6.5+` | Jakarta Persistence API 規範實作，負責基礎設施層 ORM 實體映射與事務存取 |
| **高效能連線池** | **Alibaba Druid** | `1.2.23` | 分散式資料庫連線池，提供 SQL 監控、防 SQL 注入過濾器，並與 Seata DataSourceProxy 無縫整合 |
| **關聯式資料庫** | **MySQL Server** | `8.0.33` | 多庫實體隔離 (`3307`)：<br/>• `seata_server` (TC 狀態庫)<br/>• `seata_order` (訂單庫)<br/>• `seata_storage` (庫存與 undo_log 庫)<br/>• `seata_account` (帳戶與 TCC 控制庫) |
| **分散式快取與鎖** | **Redis** | `7.2` | 高可用快取與分散式並發控制基礎設施 (`6379`) |
| **API 文檔與除錯** | **Springdoc OpenAPI (Swagger)** | `2.6.0` | 自動解析控制器產生 OpenAPI 3.0 規範文檔與互動式 Swagger UI 介面 |
| **容器化編排** | **Docker & Docker Compose** | `24+` / `v2` | 一鍵式本機中介軟體編排 (`docker-compose.yml`)，自動掛載全量 SQL 初始化指令碼 |

---

## 三、 系統全景與服務拓撲

### 1. 架構循序與拓撲圖

```mermaid
graph TB
    subgraph ClientLayer ["表現層與入口 (Gateway & Clients)"]
        Client["前端客戶端 / Postman / Swagger"]
        GW["<b>gateway-service (8080)</b><br/>Spring Cloud Gateway<br/>路由轉發 & CORS 整合"]
    end

    subgraph ServiceLayer ["業務微服務層 (DDD + Clean Architecture)"]
        TM["<b>order-service (8081)</b><br/>【TM】事務發起者 (@GlobalTransactional)<br/>【RM】訂單本地持久化 (AT 模式)"]
        RM1["<b>storage-service (8082)</b><br/>【RM】庫存微服務 (AT 模式)<br/>undo_log 自動反向補償"]
        RM2["<b>account-service (8083)</b><br/>【RM】帳戶微服務 (TCC 模式)<br/>Try 凍結 / Confirm 劃扣 / Cancel 解凍"]
    end

    subgraph MiddlewareLayer ["中介軟體基礎設施層 (Docker Compose)"]
        Nacos["<b>Nacos Server (8848)</b><br/>服務註冊發現 & 動態配置中心"]
        SeataTC["<b>Seata TC 伺服器 (8091 / 7091)</b><br/>全局事務協調器 (Netty RPC)"]
        MySQL[("<b>MySQL 8.0 (3307)</b><br/>seata_order / seata_storage<br/>seata_account / seata_server")]
        Redis[("<b>Redis 7.2 (6379)</b><br/>分散式快取 & 鎖")]
    end

    Client -->|"HTTP :8080"| GW
    GW -->|"路由 /api/order/**"| TM
    GW -->|"路由 /api/storage/**"| RM1
    GW -->|"路由 /api/account/**"| RM2

    TM -->|"OpenFeign (透傳 TX_XID)"| RM1
    TM -->|"OpenFeign (透傳 TX_XID)"| RM2

    TM & RM1 & RM2 <-->|"gRPC 服務註冊"| Nacos
    TM & RM1 & RM2 <-->|"Netty 事務協調"| SeataTC
    TM & RM1 & RM2 -->|"JPA / Druid"| MySQL
    SeataTC -->|"事務狀態持久化"| MySQL
```

### 2. 微服務端點與中介軟體通訊矩陣

| 服務名稱 | 連接埠 (Port) | 職責說明 | Swagger API 文檔 |
| :--- | :--- | :--- | :--- |
| **gateway-service** | `8080` | API 網關、全域路由分發、CORS 處理 | 統一對外入口 |
| **order-service** | `8081` | 訂單業務、發起 Seata 全局事務 (TM + AT) | `http://localhost:8081/swagger-ui.html` |
| **storage-service** | `8082` | 商品庫存扣減、AT 模式自動生成 undo_log | `http://localhost:8082/swagger-ui.html` |
| **account-service** | `8083` | 用戶資金劃扣、TCC 兩階段事務 (Try/Confirm/Cancel) | `http://localhost:8083/swagger-ui.html` |
| **Nacos Console** | `8848` | 服務註冊與發現 (帳密: `nacos` / `nacos`) | `http://localhost:8848/nacos` |
| **Seata TC Console** | `7091` | 事務協調器儀表板 (帳密: `seata` / `seata`) | `http://localhost:7091` |
| **MySQL Database** | `3307` | 資料庫實體 (帳密: `root` / `root_password`) | `localhost:3307` |

---

## 四、 業務與 RPC API 快速規格表

### 1. 跨微服務業務查詢 API (Business Query APIs)

| 微服務 | Method | 存取路徑 (經網關 / 直連) | 說明與預期回傳 |
| :--- | :--- | :--- | :--- |
| **庫存服務** | `GET` | `/api/storage/{productId}` | 查詢商品即時庫存 (`total`, `used`, `residue`) |
| **帳戶服務** | `GET` | `/api/account/{userId}` | 查詢用戶資產總覽 (`balance`, `freezeAmount`, `totalAmount`) |
| **訂單服務** | `GET` | `/api/order/{orderId}` | 查詢訂單交易狀態與明細 (`orderId`, `count`, `amount`, `status`) |

### 2. 核心分散式事務 API

* **建立訂單並開啟全局事務 (Order Service TM)**:
  * `POST /api/order/create`
  * Body: `{"userId": 10001, "productId": 20001, "count": 2, "payAmount": 500.00, "simulateError": false}`
* **庫存扣減 RPC (Storage Service AT RM)**:
  * `POST /api/storage/deduct`
  * Body: `{"productId": 20001, "count": 2}`
* **資金凍結 TCC Try (Account Service TCC RM)**:
  * `POST /api/account/tcc/prepare`
  * Body: `{"userId": 10001, "amount": 500.00}`

---

## 五、 5 分鐘快速啟動與上手指南

### 步驟 1：啟動基礎設施容器 (MySQL, Nacos, Seata TC, Redis)

確保已安裝 Docker 與 Docker Compose，在專案根目錄下執行：

```powershell
docker compose up -d
```
> 容器啟動後，MySQL 會自動執行 [scripts/sql/00_init_databases.sql](file:///d:/桌面/MonoRepository/seata-architecture/scripts/sql/00_init_databases.sql)，完成四個資料庫 (`seata_server`, `seata_order`, `seata_storage`, `seata_account`) 與初始數據的灌入。

### 步驟 2：編譯並建置所有微服務

```powershell
mvn clean install -DskipTests
```

### 步驟 3：依序啟動微服務

建議依序啟動（透過 IDE 或指令列）：
1. **`storage-service`** (`com.seata.demo.storage.StorageApplication`)
2. **`account-service`** (`com.seata.demo.account.AccountApplication`)
3. **`order-service`** (`com.seata.demo.order.OrderApplication`)
4. **`gateway-service`** (`com.seata.demo.gateway.GatewayApplication`)

---

## 六、 端到端 (E2E) 核心測試情境

詳細時序圖與測試步驟請參閱完整的 [03-end-to-end-verification-guide.md](file:///d:/桌面/MonoRepository/seata-architecture/docs/guide/03-end-to-end-verification-guide.md)。

### 情境 1：正常下單 (二階段提交 Two-Phase Commit)
```powershell
# 1. 透過網關發起下單
curl -X POST http://localhost:8080/api/order/create `
  -H "Content-Type: application/json" `
  -d '{\"userId\": 10001, \"productId\": 20001, \"count\": 2, \"payAmount\": 500.00, \"simulateError\": false}'

# 2. 驗證庫存已扣減 (residue 應為 98)
curl -X GET http://localhost:8080/api/storage/20001

# 3. 驗證帳戶已劃扣 (balance 應為 9500.00)
curl -X GET http://localhost:8080/api/account/10001
```

### 情境 2：人為異常全鏈路回滾 (二階段分散式回滾 Two-Phase Rollback)
傳入 `"simulateError": true` 模擬下單後續業務異常：
```powershell
# 1. 發起異常下單
curl -X POST http://localhost:8080/api/order/create `
  -H "Content-Type: application/json" `
  -d '{\"userId\": 10001, \"productId\": 20001, \"count\": 2, \"payAmount\": 500.00, \"simulateError\": true}'

# 2. 驗證 AT 自動反向補償 (residue 保持 100，無扣除)
curl -X GET http://localhost:8080/api/storage/20001

# 3. 驗證 TCC Cancel 自動解凍 (balance 保持 10000.00，freezeAmount 為 0)
curl -X GET http://localhost:8080/api/account/10001
```

### 情境 3：邊界防禦攔截 (庫存不足 / 餘額不足)
```powershell
# 測試庫存不足防禦 (購買 9999 件) -> 回傳 400 庫存不足
curl -X POST http://localhost:8080/api/order/create `
  -H "Content-Type: application/json" `
  -d '{\"userId\": 10001, \"productId\": 20001, \"count\": 9999, \"payAmount\": 500.00, \"simulateError\": false}'

# 測試餘額不足防禦 (金額超額) -> 回傳 400 餘額不足
curl -X POST http://localhost:8080/api/order/create `
  -H "Content-Type: application/json" `
  -d '{\"userId\": 10001, \"productId\": 20001, \"count\": 1, \"payAmount\": 9999999.00, \"simulateError\": false}'
```

---

## 七、 專案目錄與模組職責說明

```
seata-architecture/
├── .agents/
│   └── AGENTS.md                  # 專案架構規範與 Clean Architecture / DDD 準則
├── shared-module/                 # 跨微服務共享模組 (Result<T>, BusinessException, TccStatus)
├── storage-service/               # 庫存微服務 (DDD 聚合根 Storage, AT 模式 RM)
├── account-service/               # 帳戶微服務 (DDD 聚合根 Account, TCC 模式 RM)
├── order-service/                 # 訂單微服務 (DDD 聚合根 Order, TM 事務發起者)
├── gateway-service/               # API 網關微服務 (Spring Cloud Gateway 路由)
├── scripts/
│   └── sql/
│       ├── 00_init_databases.sql  # 容器初始化全量 SQL
│       ├── seata_server_db.sql    # Seata TC 伺服器資料表
│       └── undo_log.sql           # AT 模式復原日誌表
├── docs/
│   ├── guide/                     # 實戰操作與驗證指南
│   │   ├── 02-shared-module-dependency-guide.md # 跨專案引入指南
│   │   └── 03-end-to-end-verification-guide.md  # 核心端到端 (E2E) 測試手冊
│   └── seata/                     # Seata 深度機制與架構文件 (01 ~ 08)
├── docker-compose.yml             # 一鍵式中介軟體容器編排檔
└── pom.xml                        # Maven 父專案依賴管理
```

---

## 八、 深度架構文檔導覽 (Docs Index)

如需進一步瞭解底層原理與技術選型，請參閱本專案收錄的架構技術白皮書：

1. [01. Seata 架構總覽與核心機制](file:///d:/桌面/MonoRepository/seata-architecture/docs/seata/01-architecture-overview.md)
2. [02. 四大事務模式詳解 (AT, TCC, Saga, XA)](file:///d:/桌面/MonoRepository/seata-architecture/docs/seata/02-transaction-modes.md)
3. [03. AT 模式核心機制深度解構 (Before/After Image, Undo Log)](file:///d:/桌面/MonoRepository/seata-architecture/docs/seata/03-at-mode-deep-dive.md)
4. [04. TCC 模式架構與三大異常設計 (空回滾、防懸掛、冪等性)](file:///d:/桌面/MonoRepository/seata-architecture/docs/seata/04-tcc-design-patterns.md)
5. [05. 生產環境部署與高可用架構 (HA)](file:///d:/桌面/MonoRepository/seata-architecture/docs/seata/05-production-deployment-ha.md)
6. [06. 實戰整合手冊 (Spring Cloud Alibaba & MySQL)](file:///d:/桌面/MonoRepository/seata-architecture/docs/seata/06-integration-guide.md)
7. [07. Spring Boot 3.3.4 + JPA 微服務落地設計文件](file:///d:/桌面/MonoRepository/seata-architecture/docs/seata/07-spring-boot-jpa-seata-design.md)
8. [08. 微服務數量規格、中間件選型與全鏈路介接架構文件](file:///d:/桌面/MonoRepository/seata-architecture/docs/seata/08-microservices-topology-and-middleware-integration.md)
9. [端到端 (E2E) 實戰測試指南 (時序圖 / API 契約 / 資料庫前後驗證)](file:///d:/桌面/MonoRepository/seata-architecture/docs/guide/03-end-to-end-verification-guide.md)
