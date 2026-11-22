package com.seata.demo.order.infrastructure.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.seata.demo.shared.response.Result;

/**
 * 帳戶微服務 OpenFeign 客戶端 (TCC Try)
 *
 * @author Seata Architecture Demo Team
 */
@FeignClient(name = "account-service")
public interface AccountFeignClient {

    @PostMapping("/api/account/tcc/prepare")
    Result<Object> preparePayment(@RequestBody Map<String, Object> request);
}
