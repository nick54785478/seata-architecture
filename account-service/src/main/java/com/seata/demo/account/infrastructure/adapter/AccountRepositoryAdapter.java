package com.seata.demo.account.infrastructure.adapter;

import java.util.Optional;
import org.springframework.stereotype.Component;

import com.seata.demo.account.application.port.out.AccountRepositoryPort;
import com.seata.demo.account.domain.account.aggregate.root.Account;
import com.seata.demo.account.domain.account.aggregate.vo.Money;
import com.seata.demo.account.domain.account.aggregate.vo.UserId;
import com.seata.demo.account.infrastructure.persistence.account.entity.AccountJpaEntity;
import com.seata.demo.account.infrastructure.persistence.account.repository.AccountJpaRepository;

/**
 * 帳戶儲存庫 Outbound 適配器 (Account Repository Adapter)
 * 命名規範 3-9: ... + Adapter
 */
@Component
public class AccountRepositoryAdapter implements AccountRepositoryPort {

    private final AccountJpaRepository accountJpaRepository;

    public AccountRepositoryAdapter(AccountJpaRepository accountJpaRepository) {
        this.accountJpaRepository = accountJpaRepository;
    }

    @Override
    public Optional<Account> findByUserId(UserId userId) {
        return accountJpaRepository.findById(userId.getValue())
                .map(this::toDomain);
    }

    @Override
    public Account save(Account account) {
        AccountJpaEntity entity = toEntity(account);
        AccountJpaEntity savedEntity = accountJpaRepository.saveAndFlush(entity);
        return toDomain(savedEntity);
    }

    private Account toDomain(AccountJpaEntity entity) {
        return new Account(
                UserId.of(entity.getUserId()),
                Money.of(entity.getBalance()),
                Money.of(entity.getFreezeAmount()));
    }

    private AccountJpaEntity toEntity(Account domain) {
        return AccountJpaEntity.builder()
                .userId(domain.getUserId().getValue())
                .balance(domain.getBalance().getValue())
                .freezeAmount(domain.getFreezeAmount().getValue())
                .build();
    }
}
