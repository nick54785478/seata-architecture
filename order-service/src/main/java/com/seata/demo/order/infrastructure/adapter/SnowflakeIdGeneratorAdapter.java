package com.seata.demo.order.infrastructure.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.seata.demo.order.application.port.out.IdGeneratorPort;
import com.seata.demo.shared.util.SnowflakeIdGenerator;

/**
 * 雪花演算法主鍵適配器 (Snowflake ID Generator Adapter)
 * 命名規範 3-9: ... + Adapter
 * 實作應用層 IdGeneratorPort，將 shared-module 中的 SnowflakeIdGenerator 適配為 Spring Component。
 * 支援透過 Spring 配置檔自訂工作節點 (workerId) 與資料中心 (datacenterId)。
 *
 * @author Seata Architecture Demo Team
 */
@Component
public class SnowflakeIdGeneratorAdapter implements IdGeneratorPort {

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public SnowflakeIdGeneratorAdapter(
            @Value("${snowflake.worker-id:1}") long workerId,
            @Value("${snowflake.datacenter-id:1}") long datacenterId) {
        this.snowflakeIdGenerator = new SnowflakeIdGenerator(workerId, datacenterId);
    }

    @Override
    public long nextId() {
        return snowflakeIdGenerator.nextId();
    }
}
