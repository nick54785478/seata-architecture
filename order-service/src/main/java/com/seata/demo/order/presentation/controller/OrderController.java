package com.seata.demo.order.presentation.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.seata.demo.order.application.dto.command.CreateOrderCommand;
import com.seata.demo.order.application.dto.query.GetOrderQuery;
import com.seata.demo.order.application.dto.result.OrderGottenResult;
import com.seata.demo.order.application.port.in.CreateOrderUseCase;
import com.seata.demo.order.application.port.in.GetOrderUseCase;
import com.seata.demo.order.presentation.assembler.OrderResourceAssembler;
import com.seata.demo.order.presentation.resource.in.CreateOrderResource;
import com.seata.demo.order.presentation.resource.out.OrderCreatedResource;
import com.seata.demo.order.presentation.resource.out.OrderRetrievedResource;
import com.seata.demo.shared.response.Result;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 訂單表現層 REST 控制器 (Order REST Controller)
 * 接收下單請求，呼叫 CreateOrderUseCase 發起全局事務；接收查詢請求，呼叫 GetOrderUseCase 返回訂單詳情。
 */
@Tag(name = "訂單服務 API (Order Service)", description = "發起 Seata 全局分散式事務 (@GlobalTransactional)，協調整合 Storage AT 庫存扣減與 Account TCC 資金扣款，並提供訂單業務查詢")
@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase, GetOrderUseCase getOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
    }

    /**
     * 建立訂單 API (Seata 全局事務入口)
     *
     * @param resource 請求載體
     * @return 統一回應
     */
    @Operation(summary = "建立新訂單 (發起 Seata 分散式事務)", description = "建立訂單並透過 RPC 呼叫扣減庫存 (AT) 與凍結資金 (TCC)。可藉由 simulateError=true 觸發後續異常以驗證全局回滾。")
    @ApiResponse(responseCode = "200", description = "下單成功或業務處理完成")
    @ApiResponse(responseCode = "400", description = "請求參數驗證失敗")
    @ApiResponse(responseCode = "500", description = "分散式事務處理異常或已觸發回滾")
    @PostMapping("/create")
    public Result<OrderCreatedResource> createOrder(@Validated @RequestBody CreateOrderResource resource) {
        CreateOrderCommand command = OrderResourceAssembler.toCommand(resource);
        OrderGottenResult result;
        try {
            result = createOrderUseCase.createOrder(command);
        } catch (RuntimeException ex) {
            // Seata @GlobalTransactional 切面 (AdapterInvocationWrapper) 會將事務執行中的例外包裝成 RuntimeException("try to proceed invocation error", cause)
            // 自動解包還原真實例外，以利表現層與全域例外處理器精準捕捉業務語意 (如庫存不足、餘額不足或模擬異常)
            if (ex.getCause() instanceof RuntimeException runtimeCause) {
                throw runtimeCause;
            }
            throw ex;
        }
        OrderCreatedResource response = OrderResourceAssembler.toResource(result);
        return Result.success(response);
    }

    /**
     * 查詢訂單詳情 API (業務查詢 API)
     *
     * @param orderId 訂單識別碼
     * @return 統一回應
     */
    @Operation(summary = "查詢訂單詳情 (業務查詢 API)", description = "依據訂單識別碼查詢訂單資料與狀態")
    @ApiResponse(responseCode = "200", description = "查詢成功")
    @ApiResponse(responseCode = "404", description = "查無此訂單")
    @GetMapping("/{orderId}")
    public Result<OrderRetrievedResource> getOrder(@PathVariable("orderId") Long orderId) {
        GetOrderQuery query = GetOrderQuery.builder().orderId(orderId).build();
        OrderGottenResult result = getOrderUseCase.getOrder(query);
        OrderRetrievedResource response = OrderResourceAssembler.toRetrievedResource(result);
        return Result.success(response);
    }
}
