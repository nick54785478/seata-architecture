package com.seata.demo.order.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.seata.demo.order.infrastructure.persistence.order.repository.OrderJpaRepository;

/**
 * 訂單服務資料初始化配置 (Order Data Initialization Configuration)
 * 位於 Configuration 層，在微服務啟動時自動清理歷史測試訂單記錄與 undo_log，
 * 確保每一次測試皆從乾淨狀態開始。
 */
@Configuration
@ConditionalOnProperty(name = "app.init-data.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializationConfig {

    private static final Logger log = LoggerFactory.getLogger(DataInitializationConfig.class);

    /**
     * 註冊啟動初始化任務
     *
     * @param orderJpaRepository 訂單 JPA Repository
     * @param jdbcTemplate       JDBC 範本
     * @return CommandLineRunner
     */
    @Bean
    public CommandLineRunner initOrderData(
            OrderJpaRepository orderJpaRepository,
            JdbcTemplate jdbcTemplate) {
        return args -> {
            log.info("===> [DataInit] 正在執行訂單服務 (Order Service) 資料初始化與重置...");

            // 1. 清空歷史測試訂單記錄
            try {
                orderJpaRepository.deleteAllInBatch();
                log.info("===> [DataInit] 已清除 seata_order.t_order 歷史測試訂單記錄");
            } catch (Exception e) {
                log.warn("===> [DataInit] 清理 t_order 略過: {}", e.getMessage());
            }

            // 2. 清理歷史 undo_log
            try {
                jdbcTemplate.execute("DELETE FROM undo_log");
                log.info("===> [DataInit] 已清除 seata_order.undo_log 歷史事務復原記錄");
            } catch (Exception e) {
                log.warn("===> [DataInit] 清理 undo_log 略過: {}", e.getMessage());
            }

            log.info("===> [DataInit] 訂單服務資料重置成功！");
        };
    }
}
