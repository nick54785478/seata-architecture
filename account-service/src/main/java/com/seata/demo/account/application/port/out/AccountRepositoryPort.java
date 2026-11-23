package com.seata.demo.account.application.port.out;

import java.util.Optional;
import com.seata.demo.account.domain.account.aggregate.root.Account;
import com.seata.demo.account.domain.account.aggregate.vo.UserId;

/**
 * 帳戶儲存庫 Outbound Port (Account Repository Port)
 * 命名規範 3-8: ... + Port
 *
 * @author Seata Architecture Demo Team
 */
public interface AccountRepositoryPort {

    /**
     * 依據用戶 ID 查詢帳戶領域模型
     *
     * @param userId 用戶 ID 值物件
     * @return 帳戶聚合根 (Optional)
     */
    Optional<Account> findByUserId(UserId userId);

    /**
     * 保存帳戶聚合根異動 (含 JPA saveAndFlush)
     *
     * @param account 帳戶聚合根
     * @return 保存後之領域物件
     */
    Account save(Account account);
}
