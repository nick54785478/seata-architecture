package com.seata.demo.account.infrastructure.adapter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;

import com.seata.demo.account.application.port.out.TccRecordRepositoryPort;
import com.seata.demo.account.infrastructure.persistence.account.entity.TccRecordJpaEntity;
import com.seata.demo.account.infrastructure.persistence.account.repository.TccRecordJpaRepository;
import com.seata.demo.shared.constant.TccStatus;

/**
 * TCC 控制表記錄 Outbound 適配器 (TCC Record Repository Adapter)
 * 命名規範 3-9: ... + Adapter
 *
 * @author Seata Architecture Demo Team
 */
@Component
public class TccRecordRepositoryAdapter implements TccRecordRepositoryPort {

    private final TccRecordJpaRepository tccRecordJpaRepository;

    public TccRecordRepositoryAdapter(TccRecordJpaRepository tccRecordJpaRepository) {
        this.tccRecordJpaRepository = tccRecordJpaRepository;
    }

    @Override
    public Optional<TccStatus> findStatusByXidAndBranchId(String xid, Long branchId) {
        return tccRecordJpaRepository.findByXidAndBranchId(xid, branchId)
                .map(entity -> TccStatus.valueOf(entity.getStatus()));
    }

    @Override
    public BigDecimal getFreezeAmount(String xid, Long branchId) {
        return tccRecordJpaRepository.findByXidAndBranchId(xid, branchId)
                .map(TccRecordJpaEntity::getFreezeAmount)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public void saveRecord(String xid, Long branchId, Long userId, TccStatus status, BigDecimal freezeAmount) {
        TccRecordJpaEntity entity = TccRecordJpaEntity.builder()
                .xid(xid)
                .branchId(branchId)
                .userId(userId)
                .status(status.name())
                .freezeAmount(freezeAmount != null ? freezeAmount : BigDecimal.ZERO)
                .gmtCreate(LocalDateTime.now())
                .build();
        tccRecordJpaRepository.saveAndFlush(entity);
    }

    @Override
    public Optional<Long> findUserIdByXidAndBranchId(String xid, Long branchId) {
        return tccRecordJpaRepository.findByXidAndBranchId(xid, branchId)
                .map(TccRecordJpaEntity::getUserId);
    }
}
