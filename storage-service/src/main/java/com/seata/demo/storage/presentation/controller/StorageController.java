package com.seata.demo.storage.presentation.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.seata.demo.shared.response.Result;
import com.seata.demo.storage.application.dto.command.DeductStorageCommand;
import com.seata.demo.storage.application.dto.query.GetStorageQuery;
import com.seata.demo.storage.application.dto.result.StorageGottenResult;
import com.seata.demo.storage.application.port.in.DeductStorageUseCase;
import com.seata.demo.storage.application.port.in.GetStorageUseCase;
import com.seata.demo.storage.presentation.assembler.StorageResourceAssembler;
import com.seata.demo.storage.presentation.resource.in.DeductStorageResource;
import com.seata.demo.storage.presentation.resource.out.StorageDeductedResource;
import com.seata.demo.storage.presentation.resource.out.StorageRetrievedResource;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 庫存表現層 REST 控制器 (Storage REST Controller)
 * 接收 HTTP 請求，透過 Assembler 轉換為 Command/Query，呼叫 UseCase 介面並回傳統一 Result。
 *
 * @author Seata Architecture Demo Team
 */
@Tag(name = "庫存服務 API (Storage Service)", description = "提供庫存扣減與業務查詢功能，作為 Seata AT 模式資源管理器 (RM)，透過 DataSourceProxy 保障本地事務與全局事務協同")
@RestController
@RequestMapping("/api/storage")
public class StorageController {

    private final DeductStorageUseCase deductStorageUseCase;
    private final GetStorageUseCase getStorageUseCase;

    public StorageController(DeductStorageUseCase deductStorageUseCase, GetStorageUseCase getStorageUseCase) {
        this.deductStorageUseCase = deductStorageUseCase;
        this.getStorageUseCase = getStorageUseCase;
    }

    /**
     * 依據商品 ID 查詢即時庫存詳情 (業務查詢 API)
     *
     * @param productId 商品 ID
     * @return 庫存詳情統一回應
     */
    @Operation(summary = "依商品 ID 查詢庫存詳情 (業務查詢 API)", description = "取得商品的總庫存、已使用庫存與即時剩餘可用庫存。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查詢成功"),
            @ApiResponse(responseCode = "404", description = "找不到該商品庫存 (StorageNotFoundException)")
    })
    @GetMapping("/{productId}")
    public Result<StorageRetrievedResource> getStorage(@PathVariable Long productId) {
        GetStorageQuery query = GetStorageQuery.builder().productId(productId).build();
        StorageGottenResult result = getStorageUseCase.getStorage(query);
        StorageRetrievedResource response = StorageResourceAssembler.toRetrievedResource(result);
        return Result.success(response);
    }

    /**
     * 扣減商品庫存 API (參與 Seata AT 模式)
     *
     * @param resource 扣減庫存請求參數
     * @return 統一回應包裝
     */
    @Operation(summary = "扣減商品庫存 (Seata AT 分支事務)", description = "依照商品 ID 扣減指定數量之庫存。由 Seata DataSourceProxy 攔截 SQL 並寫入 undo_log 表以支援兩階段回滾。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "庫存扣減成功"),
            @ApiResponse(responseCode = "400", description = "請求參數有誤或庫存不足 (StorageNotEnoughException)"),
            @ApiResponse(responseCode = "404", description = "找不到該商品庫存 (StorageNotFoundException)")
    })
    @PostMapping("/deduct")
    public Result<StorageDeductedResource> deductStorage(@Validated @RequestBody DeductStorageResource resource) {
        DeductStorageCommand command = StorageResourceAssembler.toCommand(resource);
        deductStorageUseCase.deductStorage(command);
        StorageDeductedResource response = StorageResourceAssembler.toResource(resource.getProductId(), resource.getCount());
        return Result.success(response);
    }
}
