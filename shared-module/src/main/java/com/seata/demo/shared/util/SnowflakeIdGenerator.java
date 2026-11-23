package com.seata.demo.shared.util;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;

/**
 * 分散式雪花演算法主鍵生成器 (Twitter Snowflake ID Generator)
 * <p>
 * <b>【架構定位與設計背景】</b><br>
 * 本工具類別位於 {@code shared-module}，提供全微服務體系純 Java (Pure Java) 實作、零外部依賴、
 * 執行緒安全且具備高吞吐（單機每秒破 400 萬筆）之 64-bit 分散式唯一識別碼生成器。
 * <p>
 * <b>【為什麼在 Seata 分散式架構中捨棄資料庫自增 (AUTO_INCREMENT)？】</b><br>
 * 1. <b>避免打斷 JPA / Hibernate 批處理</b>：JPA 對於 {@code GenerationType.IDENTITY} 實體，
 *    被迫在 {@code persist()} 時立即執行單筆 SQL {@code INSERT} 以獲取回填主鍵，完全摧毀 JDBC Batching 批次效能。<br>
 * 2. <b>Seata AT 模式前置鏡像需求</b>：在發起全局事務前，實體聚合根必須具備確定性的業務主鍵，
 *    以利於後續在微服務呼叫鏈路（RPC / HTTP Header）、領域事件（Domain Event）與 {@code undo_log} 中精確鎖定記錄。<br>
 * 3. <b>捍衛 DDD 聚合根完整性</b>：聚合根在記憶體中建立之初（In-Memory Creation）就必須具備唯一標識符，
 *    而非落庫後才延遲賦予，確保領域模型生命週期不變性 (Invariants)。
 * <p>
 * <b>【64-bit 二進位結構佈局 (Bit Layout)】</b>
 * <pre>
 * +------+------------------------------------------+----------------+---------------+--------------------+
 * | 1bit |                 41bits                   |     5bits      |     5bits     |       12bits       |
 * +------+------------------------------------------+----------------+---------------+--------------------+
 * | 符號位| 時間戳毫秒差 (相對於自定義基準 Epoch)        | 資料中心機房 ID | 機器工作節點 ID| 毫秒內自增流水號   |
 * | (0)  | 0 ~ 2199023255551 (可用約 69.7 年)        | 0 ~ 31 (共32區)| 0 ~ 31 (共32台)| 0 ~ 4095 (4096筆)  |
 * +------+------------------------------------------+----------------+---------------+--------------------+
 * </pre>
 * <p>
 * <b>【時鐘回撥防禦機制 (Clock Drift Handling)】</b><br>
 * 當系統時間受 NTP（網路時間協議）伺服器同步校時而向後倒流時：
 * <ul>
 *   <li><b>微小時鐘抖動 (offset &le; 5ms)</b>：執行緒透過 {@link #wait(long)} / 自旋等待，主動追平時間戳後繼續發號。</li>
 *   <li><b>重大時鐘回撥 (offset &gt; 5ms)</b>：立即拋出 {@link IllegalStateException} 硬性阻斷，拒絕發號以防重複 ID 污染金融資產數據。</li>
 * </ul>
 * <p>
 * <b>【節點識別碼自適應推算 (Adaptive Fingerprint)】</b><br>
 * 提供無參建構子，自動抓取本機網卡 MAC 位址與 JVM 作業系統進程 PID 計算機房與節點 ID，在容器化或本地測試無配置時自動保障不衝突。
 *
 * @author Seata Architecture Demo Team
 * @see <a href="file:///d:/桌面/MonoRepository/seata-architecture/docs/seata/09-distributed-id-snowflake-design.md">09. 分散式雪花演算法技術白皮書</a>
 */
public class SnowflakeIdGenerator {

    /**
     * 自定義基準時間戳 (Custom Epoch)：2024-01-01 00:00:00 UTC (1704067200000L)
     * <p>
     * 41 位元時間戳所儲存的是自此 Epoch 起算的毫秒差值，可供系統使用：
     * 2^41 / (1000 * 60 * 60 * 24 * 365) &asymp; 69.7 年。
     */
    private final long twepoch = 1704067200000L;

    /**
     * 工作機器節點識別碼所佔用的位元數 (5 bits，支援 2^5 = 32 個節點)
     */
    private final long workerIdBits = 5L;

    /**
     * 資料中心機房識別碼所佔用的位元數 (5 bits，支援 2^5 = 32 個資料中心)
     */
    private final long datacenterIdBits = 5L;

    /**
     * 工作節點 ID 最大值 (31L)。
     * 計算方式：-1L ^ (-1L &lt;&lt; 5L) = 31
     */
    private final long maxWorkerId = ~(-1L << workerIdBits);

    /**
     * 資料中心 ID 最大值 (31L)。
     * 計算方式：-1L ^ (-1L &lt;&lt; 5L) = 31
     */
    private final long maxDatacenterId = ~(-1L << datacenterIdBits);

    /**
     * 毫秒內自增序列號所佔用的位元數 (12 bits，單一毫秒內支援 2^12 = 4096 個獨立序號)
     */
    private final long sequenceBits = 12L;

    /**
     * 工作節點 ID 向左偏移量 (12 位元，越過 sequenceBits)
     */
    private final long workerIdShift = sequenceBits;

    /**
     * 資料中心 ID 向左偏移量 (17 位元，越過 sequenceBits + workerIdBits)
     */
    private final long datacenterIdShift = sequenceBits + workerIdBits;

    /**
     * 時間戳毫秒數向左偏移量 (22 位元，越過 sequenceBits + workerIdBits + datacenterIdBits)
     */
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    /**
     * 毫秒內序號遮罩碼 (Mask: 4095L / 0xFFF)，用於避免 sequence 超過 12 位元限制，亦用於取模歸零
     */
    private final long sequenceMask = ~(-1L << sequenceBits);

    /**
     * 最大允許之時鐘回撥容忍毫秒數 (預設 5ms)
     */
    private static final long MAX_BACKWARD_MS = 5L;

    /**
     * 本節點分配之工作機器識別碼 (0 ~ 31)
     */
    private final long workerId;

    /**
     * 本節點分配之資料中心機房識別碼 (0 ~ 31)
     */
    private final long datacenterId;

    /**
     * 毫秒內自增序列號 (0 ~ 4095)，初始為 0L
     */
    private long sequence = 0L;

    /**
     * 上一次成功產生 ID 時的系統時間戳毫秒數 (初始為 -1L)
     */
    private long lastTimestamp = -1L;

    /**
     * 自適應無參建構子 (Adaptive Constructor)
     * <p>
     * 在未顯式配置環境變數或組態檔的情況下，自動採用本機硬體網卡 MAC 位址與 JVM 進程 PID 進行特徵計算，
     * 自動產生合法的 {@code datacenterId} 與 {@code workerId}，避免本機多微服務並行時發生碰撞。
     */
    public SnowflakeIdGenerator() {
        this.datacenterId = getDatacenterId(maxDatacenterId);
        this.workerId = getMaxWorkerId(datacenterId, maxWorkerId);
    }

    /**
     * 顯式指定節點識別碼之建構子 (Explicit Constructor)
     * <p>
     * 適用於生產環境透過 Spring Boot 設定檔 {@code snowflake.worker-id}、Kubernetes StatefulSet
     * 或 Redis 動態租約中心指派的唯一節點參數。
     *
     * @param workerId     工作機器識別碼，合法範圍為 0 ~ 31
     * @param datacenterId 資料中心識別碼，合法範圍為 0 ~ 31
     * @throws IllegalArgumentException 當 workerId 或 datacenterId 超過 [0, 31] 邊界時拋出
     */
    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(
                    String.format("Worker ID 超出合法範圍！數值必須介於 0 與 %d 之間，當前傳入值: %d", maxWorkerId, workerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(
                    String.format("Datacenter ID 超出合法範圍！數值必須介於 0 與 %d 之間，當前傳入值: %d", maxDatacenterId, datacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 取得下一個全域唯一分散式雪花 ID (Thread-Safe Distributed ID)
     * <p>
     * <b>【核心演算四部曲】</b>
     * <ol>
     *   <li><b>時鐘檢查與回撥防禦</b>：對比當前時間 {@code timestamp} 與 {@code lastTimestamp}，
     *       若發生倒流在 5ms 內以自旋追平，超過 5ms 則阻斷報錯。</li>
     *   <li><b>毫秒內序號遞增</b>：若處於同一毫秒，序列號 {@code sequence} 加 1 並與 {@code sequenceMask} 進行位元 AND 運算；
     *       若溢出（超過 4095）則阻塞等待至下一毫秒。</li>
     *   <li><b>跨毫秒序號歸零</b>：若時間進入新的一毫秒，將序列號重置為 0L。</li>
     *   <li><b>64-bit 位元拼裝</b>：透過位移運算 (Shift) 與位元或運算 (Bitwise OR {@code |}) 組合輸出 64-bit Long。</li>
     * </ol>
     *
     * @return 64 位元長整數唯一識別碼 (Positive Long ID)
     * @throws IllegalStateException 當偵測到嚴重的系統時鐘向後回撥（超過 5ms）時拋出
     */
    public synchronized long nextId() {
        long timestamp = timeGen();

        // 階段一：時鐘回撥檢測與防禦處置
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= MAX_BACKWARD_MS) {
                // 微小時鐘抖動 (<= 5ms)：透過等待追平時間戳
                try {
                    // 等待兩倍回撥時長以確保跨越抖動區間
                    wait(offset << 1);
                    timestamp = timeGen();
                    if (timestamp < lastTimestamp) {
                        throw new IllegalStateException(String.format(
                                "時鐘回撥自旋等待後仍未追平時間戳！上次: %d, 當前: %d, 仍落後: %d ms",
                                lastTimestamp, timestamp, lastTimestamp - timestamp));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("時鐘回撥自旋等待線程被意外中斷", e);
                }
            } else {
                // 嚴重時鐘回撥 (> 5ms)：拒絕發號，保護資料庫主鍵不重複
                throw new IllegalStateException(String.format(
                        "偵測到系統時鐘嚴重向後回撥！拒絕產生 ID 以保障資料一致性。回撥偏差毫秒數: %d ms (上次: %d, 當前: %d)",
                        offset, lastTimestamp, timestamp));
            }
        }

        // 階段二：同一毫秒內序列號自增或跨毫秒歸零
        if (lastTimestamp == timestamp) {
            // 在同一毫秒內，序號原子累加，並透過 sequenceMask 進行 4095 環形截斷
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                // 當前毫秒產生的 ID 數量已達上限 (4096 個)，自旋阻塞等待推進至下一毫秒
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 進入嶄新的毫秒週期，序號歸零重置
            sequence = 0L;
        }

        // 記錄本次成功發號的最後時間戳
        lastTimestamp = timestamp;

        // 階段三：64 位元位移拼裝組合
        // (timestamp 偏移 22 位元) | (datacenterId 偏移 17 位元) | (workerId 偏移 12 位元) | sequence
        return ((timestamp - twepoch) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    /**
     * 自旋阻塞直到系統時鐘推進至超過 {@code lastTimestamp} 的下一毫秒
     *
     * @param lastTimestamp 前次發號時間戳
     * @return 推進後的最新毫秒時間戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 取得當前系統毫秒時間戳
     *
     * @return 當前 UNIX 時間戳毫秒數
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }

    /**
     * 依據本機網路介面 MAC 位址計算資料中心機房識別碼 (Datacenter ID)
     * <p>
     * 取 MAC 位址最後兩碼位元組進行組合並取模，若網卡無效或環境異常則預設回傳 1L。
     *
     * @param maxDatacenterId 資料中心 ID 允許最大上限 (31L)
     * @return 計算得出之合法 Datacenter ID (0 ~ 31)
     */
    private static long getDatacenterId(long maxDatacenterId) {
        long id = 0L;
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            if (network == null) {
                id = 1L;
            } else {
                byte[] mac = network.getHardwareAddress();
                if (mac != null) {
                    id = ((0x000000FF & (long) mac[mac.length - 2])
                            | (0x0000FF00 & (((long) mac[mac.length - 1]) << 8))) >> 6;
                    id = id % (maxDatacenterId + 1);
                }
            }
        } catch (Exception e) {
            id = 1L;
        }
        return id;
    }

    /**
     * 依據本機 JVM 進程識別碼 (PID) 與資料中心特徵計算機器節點識別碼 (Worker ID)
     * <p>
     * 提取 {@link ManagementFactory#getRuntimeMXBean()} 之進程標識名稱（如 {@code 12345@hostname}），
     * 進行 HashCode 計算並取模，若環境異常則預設回傳 1L。
     *
     * @param datacenterId 資料中心機房 ID
     * @param maxWorkerId  工作節點 ID 允許最大上限 (31L)
     * @return 計算得出之合法 Worker ID (0 ~ 31)
     */
    private static long getMaxWorkerId(long datacenterId, long maxWorkerId) {
        StringBuilder mpid = new StringBuilder();
        mpid.append(datacenterId);
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            if (!name.isEmpty()) {
                mpid.append(name.split("@")[0]);
            }
            return (mpid.toString().hashCode() & 0xffff) % (maxWorkerId + 1);
        } catch (Exception e) {
            return 1L;
        }
    }
}
