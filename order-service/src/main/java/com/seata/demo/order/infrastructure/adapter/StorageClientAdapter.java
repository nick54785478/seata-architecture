package com.seata.demo.order.infrastructure.adapter;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

import com.seata.demo.order.application.port.out.StorageClientPort;
import com.seata.demo.order.infrastructure.client.StorageFeignClient;
import com.seata.demo.shared.response.Result;

/**
 * 遠程庫存微服務 Outbound 適配器 (Storage Client Adapter)
 * 命名規範 3-9: ... + Adapter
 *
 * @author Seata Architecture Demo Team
 */
@Component
public class StorageClientAdapter implements StorageClientPort {

    private final StorageFeignClient storageFeignClient;

    public StorageClientAdapter(StorageFeignClient storageFeignClient) {
        this.storageFeignClient = storageFeignClient;
    }

    @Override
    public void deductStorage(Long productId, Integer count) {
        Map<String, Object> request = new HashMap<>();
        request.put("productId", productId);
        request.put("count", count);

        Result<Object> result = storageFeignClient.deductStorage(request);
        if (result == null || result.getCode() != 200) {
            String msg = result != null ? result.getMessage() : "遠程庫存服務無回應";
            throw new IllegalStateException("庫存扣減失敗: " + msg);
        }
    }
}
