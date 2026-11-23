package com.seata.demo.shared.util;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;

/**
 * 分散式雪花演算法主鍵生成器 (Twitter Snowflake ID Generator)
 * <p>
 * 架構特點：
 * 1. 結構 (64 bits)：
 *    - 1 bit: 符號位 (始終為 0)
 *    - 41 bits: 時間戳毫秒數 (支援約 69 年)
 *    - 5 bits: 資料中心 ID (支援 0 ~ 31)
 *    - 5 bits: 工作節點 ID (支援 0 ~ 31)
 *    - 12 bits: 毫秒內自增序號 (支援每毫秒 4096 個 ID)
 * 2. 時鐘回撥安全機制 (Clock Drift Handling)：
 *    - 5 毫秒內之時鐘回撥自動自旋等待追平。
 *    - 超過 5 毫秒之重大回撥拋出異常阻斷，防止 ID 重複。
 * 3. 節點自動檢測 (Self-adaptive Worker/Datacenter Detection)：
 *    - 若未手動指定，依據本機網卡 MAC 與 JVM 進程 ID 自動計算，避免容器化動態部署衝突。
 *
 * @author Seata Architecture Demo Team
 */
public class SnowflakeIdGenerator {

    /**
     * 基準時間戳 (Epoch: 2024-01-01 00:00:00 UTC)
     */
    private final long twepoch = 1704067200000L;

    private final long workerIdBits = 5L;
    private final long datacenterIdBits = 5L;
    private final long maxWorkerId = ~(-1L << workerIdBits);
    private final long maxDatacenterId = ~(-1L << datacenterIdBits);
    private final long sequenceBits = 12L;

    private final long workerIdShift = sequenceBits;
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    private final long sequenceMask = ~(-1L << sequenceBits);

    private final long workerId;
    private final long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * 無參建構：自動根據網卡與進程指紋產生 workerId 與 datacenterId
     */
    public SnowflakeIdGenerator() {
        this.datacenterId = getDatacenterId(maxDatacenterId);
        this.workerId = getMaxWorkerId(datacenterId, maxWorkerId);
    }

    /**
     * 指定工作節點與資料中心 ID
     *
     * @param workerId     工作機器 ID (0 ~ 31)
     * @param datacenterId 資料中心 ID (0 ~ 31)
     */
    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 取得下一個全域唯一 ID (執行緒安全)
     *
     * @return 64 位元長整數唯一 ID
     */
    public synchronized long nextId() {
        long timestamp = timeGen();

        // 處理時鐘回撥
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                // 5ms 內時鐘抖動，自旋等待追平
                try {
                    wait(offset << 1);
                    timestamp = timeGen();
                    if (timestamp < lastTimestamp) {
                        throw new IllegalStateException(String.format("時鐘回撥等待後仍未追平, Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("時鐘回撥自旋等待時被中斷", e);
                }
            } else {
                throw new IllegalStateException(String.format("時鐘向後回撥異常！拒絕產生 ID, 偏差毫秒數: %d", offset));
            }
        }

        // 同一毫秒內自增序號
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                // 當前毫秒計數溢出，阻塞等待下一毫秒
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - twepoch) << timestampLeftShift)
                | (datacenterId << datacenterIdShift)
                | (workerId << workerIdShift)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return System.currentTimeMillis();
    }

    /**
     * 依據 MAC 位址計算資料中心 ID
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
     * 依據 JVM PID 與資料中心計算 Worker ID
     */
    private static long getMaxWorkerId(long datacenterId, long maxWorkerId) {
        StringBuilder mpid = new StringBuilder();
        mpid.append(datacenterId);
        String name = ManagementFactory.getRuntimeMXBean().getName();
        if (!name.isEmpty()) {
            mpid.append(name.split("@")[0]);
        }
        return (mpid.toString().hashCode() & 0xffff) % (maxWorkerId + 1);
    }
}
