package com.seata.demo.order.infrastructure.adapter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

import com.seata.demo.order.application.port.out.AccountClientPort;
import com.seata.demo.order.infrastructure.client.AccountFeignClient;
import com.seata.demo.shared.response.Result;

/**
 * 遠程帳戶微服務 Outbound 適配器 (Account Client Adapter)
 * 命名規範 3-9: ... + Adapter
 *
 * @author Seata Architecture Demo Team
 */
@Component
public class AccountClientAdapter implements AccountClientPort {

    private final AccountFeignClient accountFeignClient;

    public AccountClientAdapter(AccountFeignClient accountFeignClient) {
        this.accountFeignClient = accountFeignClient;
    }

    @Override
    public void preparePayment(Long userId, BigDecimal amount) {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        request.put("amount", amount);

        Result<Object> result = accountFeignClient.preparePayment(request);
        if (result == null || result.getCode() != 200) {
            String msg = result != null ? result.getMessage() : "遠程帳戶服務無回應";
            throw new IllegalStateException("帳戶金額凍結失敗: " + msg);
        }
    }
}
