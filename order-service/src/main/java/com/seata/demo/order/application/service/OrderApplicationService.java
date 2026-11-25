package com.seata.demo.order.application.service;

import com.seata.demo.order.application.assembler.OrderDtoAssembler;
import com.seata.demo.order.application.dto.command.CreateOrderCommand;
import com.seata.demo.order.application.dto.query.GetOrderQuery;
import com.seata.demo.order.application.dto.result.OrderGottenResult;
import com.seata.demo.order.application.port.in.CreateOrderUseCase;
import com.seata.demo.order.application.port.in.GetOrderUseCase;
import com.seata.demo.order.application.port.out.AccountClientPort;
import com.seata.demo.order.application.port.out.IdGeneratorPort;
import com.seata.demo.order.application.port.out.OrderRepositoryPort;
import com.seata.demo.order.application.port.out.StorageClientPort;
import com.seata.demo.order.domain.order.aggregate.root.Order;
import com.seata.demo.order.domain.order.aggregate.vo.Money;
import com.seata.demo.order.domain.order.aggregate.vo.OrderId;
import com.seata.demo.order.domain.order.aggregate.vo.ProductId;
import com.seata.demo.order.domain.order.aggregate.vo.Quantity;
import com.seata.demo.order.domain.order.aggregate.vo.UserId;
import com.seata.demo.order.domain.order.exception.OrderNotFoundException;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 訂單應用層服務 (Order Application Service)
 * 命名規範 3-9: ... + ApplicationService
 * 實作 CreateOrderUseCase 與 GetOrderUseCase，發起 Seata 全局事務 (@GlobalTransactional)，擔任 TM 協調者。
 */
@Service
public class OrderApplicationService implements CreateOrderUseCase, GetOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(OrderApplicationService.class);

    private final OrderRepositoryPort orderRepositoryPort;
    private final StorageClientPort storageClientPort;
    private final AccountClientPort accountClientPort;
    private final IdGeneratorPort idGeneratorPort;

    public OrderApplicationService(OrderRepositoryPort orderRepositoryPort,
                                   StorageClientPort storageClientPort,
                                   AccountClientPort accountClientPort,
                                   IdGeneratorPort idGeneratorPort) {
        this.orderRepositoryPort = orderRepositoryPort;
        this.storageClientPort = storageClientPort;
        this.accountClientPort = accountClientPort;
        this.idGeneratorPort = idGeneratorPort;
    }

    /**
     * 開啟全局事務 (TM 角色)
     * 1. 調用庫存微服務扣庫存 (AT 模式)
     * 2. 調用帳戶微服務凍結金額 (TCC 模式 Try)
     * 3. 本地持久化訂單實體 (AT 模式 JPA，採用分散式雪花演算法生成主鍵)
     * 4. 支援人為模擬異常，觸發全鏈路分散式回滾
     */
    @Override
    @GlobalTransactional(name = "create-order-tx", rollbackFor = Exception.class, timeoutMills = 30000)
    public OrderGottenResult createOrder(CreateOrderCommand command) {
        String xid = RootContext.getXID();
        log.info("[OrderApplicationService] 開啟全局事務, XID = {}, UserId = {}, ProductId = {}, Count = {}, Amount = {}",
                xid, command.getUserId(), command.getProductId(), command.getCount(), command.getPayAmount());

        // 1. 遠程調用庫存服務 (AT 模式)
        log.info("[OrderApplicationService] 遠程調用庫存服務扣減庫存...");
        storageClientPort.deductStorage(command.getProductId(), command.getCount());

        // 2. 遠程調用帳戶服務 (TCC 模式 Try)
        log.info("[OrderApplicationService] 遠程調用帳戶服務預備凍結資金 (TCC Try)...");
        accountClientPort.preparePayment(command.getUserId(), command.getPayAmount());

        // 3. 本地持久化訂單實體 (透過雪花演算法產生唯一業務主鍵)
        long generatedOrderId = idGeneratorPort.nextId();
        Order order = Order.create(
                OrderId.of(generatedOrderId),
                UserId.of(command.getUserId()),
                ProductId.of(command.getProductId()),
                Quantity.of(command.getCount()),
                Money.of(command.getPayAmount())
        );

        orderRepositoryPort.save(order);
        log.info("[OrderApplicationService] 訂單持久化成功, OrderId = {}", order.getId().getValue());

        // 4. 模擬異常測試：若傳入 simulateError=true，拋出例外以展示全鏈路自動回滾
        if (Boolean.TRUE.equals(command.getSimulateError())) {
            log.error("[OrderApplicationService] 觸發模擬異常！即將回滾全局事務！XID = {}", xid);
            throw new RuntimeException("【模擬業務異常】觸發 Seata 全局分散式事務回滾！庫存將自動恢復，帳戶凍結金額將自動解凍！");
        }

        log.info("[OrderApplicationService] 全局事務執行完畢，準備提交, XID = {}", xid);
        return OrderDtoAssembler.toResult(order);
    }

    /**
     * 依據訂單識別碼查詢訂單業務資料
     *
     * @param query 查詢條件物件
     * @return 訂單查詢結果 DTO
     */
    @Override
    @Transactional(readOnly = true)
    public OrderGottenResult getOrder(GetOrderQuery query) {
        log.info("[OrderApplicationService] 查詢訂單詳情, OrderId = {}", query.getOrderId());
        Order order = orderRepositoryPort.findById(OrderId.of(query.getOrderId()))
                .orElseThrow(() -> new OrderNotFoundException(query.getOrderId()));
        return OrderDtoAssembler.toResult(order);
    }
}
