package com.seata.demo.order.infrastructure.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.seata.demo.shared.response.Result;

/**
 * 庫存微服務 OpenFeign 客戶端
 **/
@FeignClient(name = "storage-service")
public interface StorageFeignClient {

    @PostMapping("/api/storage/deduct")
    Result<Object> deductStorage(@RequestBody Map<String, Object> request);
}
