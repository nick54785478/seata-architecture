# 06. 實戰整合手冊 (Spring Cloud Alibaba & MySQL)

本手冊示範如何在 Spring Cloud Alibaba 微服務專案中快速整合 Seata AT 模式。

---

## 1. 基礎準備工作

1. **部署 Seata Server (TC)**：啟動 TC 並註冊到 Nacos。
2. **初始化 TC 儲存資料庫**：執行 [seata_server_db.sql](file:///d:/桌面/MonoRepository/seata-architecture/scripts/sql/seata_server_db.sql)。
3. **初始化業務資料庫**：在所有參與分支事務的業務資料庫中執行 [undo_log.sql](file:///d:/桌面/MonoRepository/seata-architecture/scripts/sql/undo_log.sql)。

---

## 2. 引入 Maven 依賴

在各微服務的 `pom.xml` 中引入 Spring Cloud Alibaba Seata Starter：

```xml
<dependencies>
    <!-- Spring Cloud Alibaba Seata -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
        <version>2022.0.0.0</version>
        <exclusions>
            <!-- 排除自帶版本，自定義對齊生產 Seata 版本 -->
            <exclusion>
                <groupId>io.seata</groupId>
                <artifactId>seata-spring-boot-starter</artifactId>
            </exclusion>
        </exclusions>
    </dependency>

    <!-- 指定 Seata 核心版本 -->
    <dependency>
        <groupId>io.seata</groupId>
        <artifactId>seata-spring-boot-starter</artifactId>
        <version>1.7.1</version>
    </dependency>
</dependencies>
```

---

## 3. 客戶端配置 (`bootstrap.yml` / `application.yml`)

```yaml
seata:
  enabled: true
  application-id: ${spring.application.name}
  # 事務分組名稱 (自定義)
  tx-service-group: default_tx_group
  service:
    # 事務分組與 TC 叢集名稱映射
    vgroup-mapping:
      default_tx_group: default
    # 禁用降級
    disable-global-transaction: false
  registry:
    type: nacos
    nacos:
      application: seata-server
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
      cluster: default
  config:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
```

---

## 4. 業務代碼使用範例

### 4.1 全局事務發起方（訂單服務 TM）

只需要在業務調用入口的方法上加上 `@GlobalTransactional`：

```java
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private StockFeignClient stockFeignClient;
    @Autowired
    private AccountFeignClient accountFeignClient;

    /**
     * 開啟全局事務
     * name: 自定義事務名稱
     * rollbackFor: 指定觸發回滾的異常類型 (預設包含所有 Exception)
     */
    @Override
    @GlobalTransactional(name = "create-order-tx", rollbackFor = Exception.class)
    public Long createOrder(OrderDTO dto) {
        log.info("開始全局事務, XID = {}", RootContext.getXID());

        // 1. 扣減庫存 (Feign 調用下游 Stock 服務)
        stockFeignClient.deductStock(dto.getProductId(), dto.getCount());

        // 2. 扣減餘額 (Feign 調用下游 Account 服務)
        accountFeignClient.debitAccount(dto.getUserId(), dto.getAmount());

        // 3. 建立訂單 (本地 DB 插入)
        OrderEntity order = new OrderEntity();
        order.setUserId(dto.getUserId());
        order.setProductId(dto.getProductId());
        order.setStatus("PAID");
        orderMapper.insert(order);

        // 模擬異常：只要在此處拋出異常，全鏈路庫存與金額都會透過 Undo Log 自動回滾
        if (dto.isSimulateError()) {
            throw new RuntimeException("下單異常，觸發全域事務回滾！");
        }

        return order.getId();
    }
}
```

### 4.2 分支事務執行方（庫存服務 / 帳戶服務 RM）

分支事務無需標註 `@GlobalTransactional`，只需常規本地 `@Transactional` 即可：

```java
@Service
public class StockServiceImpl implements StockService {

    @Autowired
    private StockMapper stockMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(Long productId, Integer count) {
        log.info("執行分支事務扣減庫存, XID = {}", RootContext.getXID());
        // Seata DataSourceProxy 會自動攔截此 SQL，記錄前後鏡像並生成 undo_log
        int rows = stockMapper.decreaseStock(productId, count);
        if (rows <= 0) {
            throw new IllegalStateException("庫存不足！");
        }
    }
}
```

---

## 5. 常見生產避坑指南 (Best Practices)

1. **主鍵設計規範**：
   - AT 模式依賴主鍵定位唯一記錄，**業務表必須有明確的主鍵（Primary Key）**。
2. **避免跨服務循環依賴**：
   - 服務 A 調用服務 B，服務 B 又調用回服務 A，可能引起全局鎖自旋死鎖。
3. **RPC 超時時間設定**：
   - Feign / HTTP 客戶端調用超時時間應小於 Seata 全局事務的 `timeout`，避免 HTTP 斷開連接但 TC 仍在等待分支匯報。
4. **分佈式鎖與熱點行競爭**：
   - 秒殺等極端高併發扣減同一行庫存場景，AT 模式全局鎖競爭會非常激烈，建議採用 **Redis 預扣庫存 + RocketMQ 最終一致性** 或 **TCC 分段庫存** 方案。
