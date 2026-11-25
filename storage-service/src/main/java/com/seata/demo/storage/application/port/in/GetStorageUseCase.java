package com.seata.demo.storage.application.port.in;

import com.seata.demo.storage.application.dto.query.GetStorageQuery;
import com.seata.demo.storage.application.dto.result.StorageGottenResult;

/**
 * 查詢商品庫存業務 UseCase Inbound Port (Get Storage UseCase)
 * 命名規範 3-8: in : ... + UseCase
 * 定義查詢商品庫存詳情之業務合約。
 */
public interface GetStorageUseCase {

    /**
     * 依商品 ID 查詢商品庫存詳情
     *
     * @param query 查詢條件物件
     * @return 庫存查詢結果 DTO
     */
    StorageGottenResult getStorage(GetStorageQuery query);
}
