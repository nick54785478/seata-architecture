package com.seata.demo.account.application.service;

import java.math.BigDecimal;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.seata.demo.account.application.assembler.AccountDtoAssembler;
import com.seata.demo.account.application.dto.command.CancelAccountPaymentCommand;
import com.seata.demo.account.application.dto.command.ConfirmAccountPaymentCommand;
import com.seata.demo.account.application.dto.command.PrepareAccountPaymentCommand;
import com.seata.demo.account.application.dto.query.GetAccountQuery;
import com.seata.demo.account.application.dto.result.AccountGottenResult;
import com.seata.demo.account.application.port.in.CancelAccountPaymentUseCase;
import com.seata.demo.account.application.port.in.ConfirmAccountPaymentUseCase;
import com.seata.demo.account.application.port.in.GetAccountUseCase;
import com.seata.demo.account.application.port.in.PrepareAccountPaymentUseCase;
import com.seata.demo.account.application.port.out.AccountRepositoryPort;
import com.seata.demo.account.application.port.out.TccRecordRepositoryPort;
import com.seata.demo.account.application.tcc.AccountTccAction;
import com.seata.demo.account.domain.account.aggregate.root.Account;
import com.seata.demo.account.domain.account.aggregate.vo.Money;
import com.seata.demo.account.domain.account.aggregate.vo.UserId;
import com.seata.demo.account.domain.account.exception.AccountNotFoundException;
import com.seata.demo.account.domain.account.exception.BalanceInsufficientException;
import com.seata.demo.shared.constant.TccStatus;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;

/**
 * 帳戶應用層服務 (Account Application Service)
 * 命名規範 3-9: ... + ApplicationService
 * 實作 Prepare/Confirm/Cancel 業務案例、GetAccountUseCase 與 Seata AccountTccAction 介面。
 * 封裝防空回滾、防業務懸掛與二階段冪等性控制邏輯。
 *
 * @author Seata Architecture Demo Team
 */
@Service
public class AccountApplicationService implements 
        PrepareAccountPaymentUseCase, 
        ConfirmAccountPaymentUseCase, 
        CancelAccountPaymentUseCase,
        GetAccountUseCase,
        AccountTccAction {

    private static final Logger log = LoggerFactory.getLogger(AccountApplicationService.class);

    private final AccountRepositoryPort accountRepositoryPort;
    private final TccRecordRepositoryPort tccRecordRepositoryPort;

    public AccountApplicationService(AccountRepositoryPort accountRepositoryPort, 
                                     TccRecordRepositoryPort tccRecordRepositoryPort) {
        this.accountRepositoryPort = accountRepositoryPort;
        this.tccRecordRepositoryPort = tccRecordRepositoryPort;
    }

    /**
     * 依據用戶 ID 查詢帳戶資產詳情
     *
     * @param query 查詢條件物件
     * @return 帳戶查詢結果 DTO
     */
    @Override
    @Transactional(readOnly = true)
    public AccountGottenResult getAccount(GetAccountQuery query) {
        log.info("[AccountApplicationService] 查詢帳戶資產, UserId = {}", query.getUserId());
        Account account = accountRepositoryPort.findByUserId(UserId.of(query.getUserId()))
                .orElseThrow(() -> new AccountNotFoundException(query.getUserId()));
        return AccountDtoAssembler.toResult(account);
    }

    // =========================================================================
    // 1. TCC 一階段：Prepare / Try
    // =========================================================================

    @Override
    public boolean prepare(
            BusinessActionContext context, 
            @BusinessActionContextParameter(paramName = "userId") Long userId, 
            @BusinessActionContextParameter(paramName = "amount") BigDecimal amount) {
        String xid = context != null ? context.getXid() : "LOCAL_TEST_XID";
        long branchId = context != null ? context.getBranchId() : 1L;

        PrepareAccountPaymentCommand command = PrepareAccountPaymentCommand.builder()
                .xid(xid)
                .branchId(branchId)
                .userId(userId)
                .amount(amount)
                .build();

        return preparePayment(command);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean preparePayment(PrepareAccountPaymentCommand command) {
        String xid = command.getXid();
        Long branchId = command.getBranchId();
        Long userId = command.getUserId();
        BigDecimal amount = command.getAmount();

        log.info("[Account TCC Try] 開始凍結資金. XID = {}, BranchID = {}, UserId = {}, Amount = {}",
                 xid, branchId, userId, amount);

        // 1. 防業務懸掛校驗：檢查是否已存在 Cancel 狀態記錄
        Optional<TccStatus> statusOpt = tccRecordRepositoryPort.findStatusByXidAndBranchId(xid, branchId);
        if (statusOpt.isPresent() && statusOpt.get() == TccStatus.CANCELED) {
            log.warn("[Account TCC Try] 檢測到已存在 CANCELED 記錄 (Cancel 早於 Try 到達)，拒絕執行 Try，防止業務懸掛！XID = {}", xid);
            return false;
        }

        // 2. 冪等性校驗：若已為 TRIED，不重複執行
        if (statusOpt.isPresent() && statusOpt.get() == TccStatus.TRIED) {
            log.info("[Account TCC Try] 已經執行過 Try，冪等返回成功. XID = {}", xid);
            return true;
        }

        // 3. 獲取帳戶聚合根並執行領域行為
        Account account = accountRepositoryPort.findByUserId(UserId.of(userId))
                .orElseThrow(() -> new BalanceInsufficientException("查無此用戶帳戶, UserId: " + userId));

        account.freeze(Money.of(amount));

        // 4. 保存領域聚合根與寫入 TCC 控制表記錄 (狀態為 TRIED，包含 userId)
        accountRepositoryPort.save(account);
        tccRecordRepositoryPort.saveRecord(xid, branchId, userId, TccStatus.TRIED, amount);

        log.info("[Account TCC Try] 資金凍結成功！UserId = {}, 凍結金額 = {}", userId, amount);
        return true;
    }

    // =========================================================================
    // 2. TCC 二階段：Confirm
    // =========================================================================

    @Override
    public boolean confirm(BusinessActionContext context) {
        String xid = context.getXid();
        long branchId = context.getBranchId();

        Long userId = null;
        BigDecimal amount = null;

        if (context.getActionContext() != null) {
            Object userIdObj = context.getActionContext("userId");
            if (userIdObj != null) {
                userId = Long.valueOf(userIdObj.toString());
            }
            Object amountObj = context.getActionContext("amount");
            if (amountObj != null) {
                amount = new BigDecimal(amountObj.toString());
            }
        }

        // 雙重安全兜底：若 Seata ActionContext 遺失，從本地 TCC 控制表記錄中讀取
        if (userId == null) {
            userId = tccRecordRepositoryPort.findUserIdByXidAndBranchId(xid, branchId).orElse(null);
        }
        if (amount == null) {
            amount = tccRecordRepositoryPort.getFreezeAmount(xid, branchId);
        }

        log.info("[Account TCC Confirm Context] XID = {}, BranchID = {}, UserId = {}, Amount = {}",
                xid, branchId, userId, amount);

        if (userId == null) {
            log.warn("[Account TCC Confirm] 檢測到無效或歷史殘留之分支事務 (無法獲取 userId 且本地無 Try 記錄)，直接返回 true 以終止 Seata TC 無限重試. XID = {}, BranchID = {}", xid, branchId);
            return true;
        }

        ConfirmAccountPaymentCommand command = ConfirmAccountPaymentCommand.builder()
                .xid(xid)
                .branchId(branchId)
                .userId(userId)
                .amount(amount != null ? amount : BigDecimal.ZERO)
                .build();

        return confirmPayment(command);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmPayment(ConfirmAccountPaymentCommand command) {
        String xid = command.getXid();
        Long branchId = command.getBranchId();
        Long userId = command.getUserId();
        BigDecimal amount = command.getAmount();

        log.info("[Account TCC Confirm] 開始正式劃扣. XID = {}, BranchID = {}, UserId = {}, Amount = {}",
                 xid, branchId, userId, amount);

        // 1. 冪等性校驗與空提交處理
        Optional<TccStatus> statusOpt = tccRecordRepositoryPort.findStatusByXidAndBranchId(xid, branchId);
        if (!statusOpt.isPresent()) {
            log.warn("[Account TCC Confirm] 空提交處理：未找到 Try 記錄 (可能資料已初始化或未執行 Try)，直接返回 true 終止 Seata TC 重試. XID = {}", xid);
            return true;
        }
        if (statusOpt.get() == TccStatus.CONFIRMED) {
            log.info("[Account TCC Confirm] 已經確認過，直接冪等返回成功. XID = {}", xid);
            return true;
        }

        // 2. 獲取聚合根並扣除凍結金額
        Account account = accountRepositoryPort.findByUserId(UserId.of(userId))
                .orElseThrow(() -> new IllegalStateException("用戶帳戶不存在, UserId: " + userId));

        account.deductFrozen(Money.of(amount));

        // 3. 保存聚合根與更新狀態為 CONFIRMED (包含 userId)
        accountRepositoryPort.save(account);
        tccRecordRepositoryPort.saveRecord(xid, branchId, userId, TccStatus.CONFIRMED, amount);

        log.info("[Account TCC Confirm] 正式劃扣完成！XID = {}", xid);
        return true;
    }

    // =========================================================================
    // 3. TCC 二階段：Cancel
    // =========================================================================

    @Override
    public boolean cancel(BusinessActionContext context) {
        String xid = context.getXid();
        long branchId = context.getBranchId();

        Long userId = null;
        BigDecimal amount = null;

        if (context.getActionContext() != null) {
            Object userIdObj = context.getActionContext("userId");
            if (userIdObj != null) {
                userId = Long.valueOf(userIdObj.toString());
            }
            Object amountObj = context.getActionContext("amount");
            if (amountObj != null) {
                amount = new BigDecimal(amountObj.toString());
            }
        }

        // 雙重安全兜底：若 Seata ActionContext 遺失，從本地 TCC 控制表記錄中讀取
        if (userId == null) {
            userId = tccRecordRepositoryPort.findUserIdByXidAndBranchId(xid, branchId).orElse(null);
        }
        if (amount == null) {
            amount = tccRecordRepositoryPort.getFreezeAmount(xid, branchId);
        }

        log.info("[Account TCC Cancel Context] XID = {}, BranchID = {}, UserId = {}, Amount = {}",
                xid, branchId, userId, amount);

        CancelAccountPaymentCommand command = CancelAccountPaymentCommand.builder()
                .xid(xid)
                .branchId(branchId)
                .userId(userId)
                .amount(amount != null ? amount : BigDecimal.ZERO)
                .build();

        return cancelPayment(command);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelPayment(CancelAccountPaymentCommand command) {
        String xid = command.getXid();
        Long branchId = command.getBranchId();
        Long userId = command.getUserId();
        BigDecimal amount = command.getAmount();

        log.info("[Account TCC Cancel] 開始解凍退款. XID = {}, BranchID = {}, UserId = {}, Amount = {}",
                 xid, branchId, userId, amount);

        Optional<TccStatus> statusOpt = tccRecordRepositoryPort.findStatusByXidAndBranchId(xid, branchId);

        // 1. 防空回滾處理：若沒有任何記錄，代表 Try 階段根本未被呼叫 (可能因網路中斷)
        if (!statusOpt.isPresent()) {
            log.warn("[Account TCC Cancel] 未檢測到 Try 記錄，觸發防空回滾！預先插入 CANCELED 狀態以防止後續延遲的 Try 懸掛. XID = {}", xid);
            tccRecordRepositoryPort.saveRecord(xid, branchId, userId, TccStatus.CANCELED, BigDecimal.ZERO);
            return true;
        }

        // 2. 冪等性校驗：若已為 CANCELED，直接返回成功
        if (statusOpt.get() == TccStatus.CANCELED) {
            log.info("[Account TCC Cancel] 已經取消過，直接冪等返回成功. XID = {}", xid);
            return true;
        }

        // 3. 獲取已凍結之實際金額並解凍退回可用餘額
        BigDecimal actualFrozen = tccRecordRepositoryPort.getFreezeAmount(xid, branchId);
        if (actualFrozen == null || actualFrozen.compareTo(BigDecimal.ZERO) == 0) {
            actualFrozen = amount;
        }

        if (userId != null && actualFrozen.compareTo(BigDecimal.ZERO) > 0) {
            Account account = accountRepositoryPort.findByUserId(UserId.of(userId))
                    .orElseThrow(() -> new IllegalStateException("用戶帳戶不存在, UserId: " + userId));

            account.unfreeze(Money.of(actualFrozen));
            accountRepositoryPort.save(account);
        }

        // 4. 更新狀態為 CANCELED (包含 userId)
        tccRecordRepositoryPort.saveRecord(xid, branchId, userId, TccStatus.CANCELED, BigDecimal.ZERO);

        log.info("[Account TCC Cancel] 解凍退回成功！XID = {}, 退款金額 = {}", xid, actualFrozen);
        return true;
    }
}
