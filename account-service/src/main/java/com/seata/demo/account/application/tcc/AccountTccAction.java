package com.seata.demo.account.application.tcc;

import java.math.BigDecimal;
import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * Seata TCC 二階段業務合約介面 (Account TCC Action)
 * 標註 @LocalTCC，定義 Try / Confirm / Cancel 三階段方法簽名。
 *
 * @author Seata Architecture Demo Team
 */
@LocalTCC
public interface AccountTccAction {

    /**
     * 一階段 Try: 預留凍結資金
     *
     * @param context Seata 上下文
     * @param userId  用戶 ID
     * @param amount  凍結金額
     * @return 是否成功
     */
    @TwoPhaseBusinessAction(
            name = "accountPaymentTccAction",
            commitMethod = "confirm",
            rollbackMethod = "cancel"
    )
    boolean prepare(
            BusinessActionContext context,
            @BusinessActionContextParameter(paramName = "userId") Long userId,
            @BusinessActionContextParameter(paramName = "amount") BigDecimal amount
    );

    /**
     * 二階段 Confirm: 正式劃扣凍結資金
     *
     * @param context Seata 上下文 (包含一階段透傳參數)
     * @return 是否成功
     */
    boolean confirm(BusinessActionContext context);

    /**
     * 二階段 Cancel: 解凍資金退回可用餘額
     *
     * @param context Seata 上下文
     * @return 是否成功
     */
    boolean cancel(BusinessActionContext context);
}
