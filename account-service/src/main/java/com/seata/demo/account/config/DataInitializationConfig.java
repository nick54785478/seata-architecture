package com.seata.demo.account.config;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.seata.demo.account.infrastructure.persistence.account.entity.AccountJpaEntity;
import com.seata.demo.account.infrastructure.persistence.account.repository.AccountJpaRepository;
import com.seata.demo.account.infrastructure.persistence.account.repository.TccRecordJpaRepository;

/**
 * 帳戶服務資料初始化配置 (Account Data Initialization Configuration)
 * 位於 Configuration 層，在微服務啟動時自動確保用戶帳戶資金回歸初始狀態、清理歷史 TCC 事務記錄與 undo_log，
 * 保證每次分散式事務測試皆具備乾淨可預測的初始數據。
 *
 * @author Seata Architecture Demo Team
 */
@Configuration
@ConditionalOnProperty(name = "app.init-data.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializationConfig {

    private static final Logger log = LoggerFactory.getLogger(DataInitializationConfig.class);

    /**
     * 測試預設用戶 ID
     */
    private static final Long DEFAULT_USER_ID = 10001L;

    /**
     * 測試預設初始可用餘額 (10,000 元)
     */
    private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("10000.00");

    /**
     * 註冊啟動初始化任務
     *
     * @param accountJpaRepository 帳戶 JPA Repository
     * @param tccRecordJpaRepository TCC 控制記錄 JPA Repository
     * @param jdbcTemplate JDBC 範本
     * @return CommandLineRunner
     */
    @Bean
    public CommandLineRunner initAccountData(
            AccountJpaRepository accountJpaRepository,
            TccRecordJpaRepository tccRecordJpaRepository,
            JdbcTemplate jdbcTemplate) {
        return args -> {
            log.info("===> [DataInit] 正在執行帳戶服務 (Account Service) 資料初始化與重置...");

            // 1. 初始化或重置預設測試帳戶 (userId: 10001, balance: 10000.00, freezeAmount: 0.00)
            AccountJpaEntity account = accountJpaRepository.findById(DEFAULT_USER_ID)
                    .orElseGet(() -> AccountJpaEntity.builder()
                            .userId(DEFAULT_USER_ID)
                            .build());

            account.setBalance(DEFAULT_BALANCE);
            account.setFreezeAmount(BigDecimal.ZERO);
            accountJpaRepository.saveAndFlush(account);

            // 2. 清空歷史 TCC 控制記錄表，避免舊有 TRIED/CONFIRMED 記錄干擾後續二階段測試
            try {
                tccRecordJpaRepository.deleteAllInBatch();
                log.info("===> [DataInit] 已清除 seata_account.t_tcc_transaction_record 歷史 TCC 事務記錄");
            } catch (Exception e) {
                log.warn("===> [DataInit] 清理 t_tcc_transaction_record 略過: {}", e.getMessage());
            }

            // 3. 清理歷史 undo_log
            try {
                jdbcTemplate.execute("DELETE FROM undo_log");
                log.info("===> [DataInit] 已清除 seata_account.undo_log 歷史事務復原記錄");
            } catch (Exception e) {
                log.warn("===> [DataInit] 清理 undo_log 略過: {}", e.getMessage());
            }

            log.info("===> [DataInit] 帳戶服務資料重置成功！用戶ID: {}, 可用餘額: {}, 凍結金額: {}",
                    DEFAULT_USER_ID, account.getBalance(), account.getFreezeAmount());
        };
    }
}
