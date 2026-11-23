package com.seata.demo.order.application.port.out;

/**
 * 分散式唯一識別碼生成 Outbound Port 介面 (ID Generator Port)
 * 命名規範 3-8: ... + Port
 * 定義應用層對主鍵生成能力的需求合約，與具體實作技術（雪花演算法、Redis、號段等）解耦。
 *
 * @author Seata Architecture Demo Team
 */
public interface IdGeneratorPort {

    /**
     * 生成下一個全域唯一分散式 ID
     *
     * @return 64 位元長整數唯一 ID
     */
    long nextId();
}
