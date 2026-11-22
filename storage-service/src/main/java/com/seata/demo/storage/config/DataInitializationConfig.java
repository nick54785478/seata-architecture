package com.seata.demo.storage.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.seata.demo.storage.infrastructure.persistence.storage.entity.StorageJpaEntity;
import com.seata.demo.storage.infrastructure.persistence.storage.repository.StorageJpaRepository;

/**
 * 庫存服務資料初始化配置 (Storage Data Initialization Configuration)
 * 位於 Configuration 層，在微服務啟動時自動確保商品庫存資料回歸初始狀態，並清理歷史 undo_log，
 * 保證分散式事務測試每次啟動皆能有乾淨可預期的初始資料。
 *
 * @author Seata Architecture Demo Team
 */
@Configuration
@ConditionalOnProperty(name = "app.init-data.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializationConfig {

    private static final Logger log = LoggerFactory.getLogger(DataInitializationConfig.class);

    /**
     * 測試預設商品 ID
     */
    private static final Long DEFAULT_PRODUCT_ID = 20001L;

    /**
     * 測試預設初始總庫存
     */
    private static final int DEFAULT_TOTAL_STORAGE = 100;

    /**
     * 註冊啟動初始化任務
     *
     * @param storageJpaRepository 庫存 JPA Repository
     * @param jdbcTemplate JDBC 範本
     * @return CommandLineRunner
     */
    @Bean
    public CommandLineRunner initStorageData(
            StorageJpaRepository storageJpaRepository,
            JdbcTemplate jdbcTemplate) {
        return args -> {
            log.info("===> [DataInit] 正在執行庫存服務 (Storage Service) 資料初始化與重置...");

            // 1. 初始化或重置測試商品庫存 (productId: 20001, total: 100, used: 0, residue: 100)
            StorageJpaEntity storage = storageJpaRepository.findByProductId(DEFAULT_PRODUCT_ID)
                    .orElseGet(() -> StorageJpaEntity.builder()
                            .productId(DEFAULT_PRODUCT_ID)
                            .build());

            storage.setTotal(DEFAULT_TOTAL_STORAGE);
            storage.setUsed(0);
            storage.setResidue(DEFAULT_TOTAL_STORAGE);
            storageJpaRepository.saveAndFlush(storage);

            // 2. 清理歷史 undo_log 表，避免舊交易殘留日誌影響 Seata AT 模式
            try {
                jdbcTemplate.execute("DELETE FROM undo_log");
                log.info("===> [DataInit] 已清除 seata_storage.undo_log 歷史事務復原記錄");
            } catch (Exception e) {
                log.warn("===> [DataInit] 清理 undo_log 略過: {}", e.getMessage());
            }

            log.info("===> [DataInit] 庫存服務資料重置成功！商品ID: {}, 總庫存: {}, 已用: {}, 剩餘: {}",
                    DEFAULT_PRODUCT_ID, storage.getTotal(), storage.getUsed(), storage.getResidue());
        };
    }
}
