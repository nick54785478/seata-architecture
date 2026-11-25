package com.seata.demo.order.domain.order.aggregate.root;

import com.seata.demo.order.domain.order.aggregate.vo.Money;
import com.seata.demo.order.domain.order.aggregate.vo.OrderId;
import com.seata.demo.order.domain.order.aggregate.vo.OrderStatus;
import com.seata.demo.order.domain.order.aggregate.vo.ProductId;
import com.seata.demo.order.domain.order.aggregate.vo.Quantity;
import com.seata.demo.order.domain.order.aggregate.vo.UserId;
import com.seata.demo.order.domain.order.event.OrderCreatedEvent;

/**
 * 訂單聚合根 (Order Aggregate Root)
 * 純 Java 實作，遵循 DDD 規範：嚴禁使用 Lombok 註解，屬性完全封裝，透過具備業務語意的行為方法進行異動。
 */
public class Order {

    private final OrderId id;
    private final UserId userId;
    private final ProductId productId;
    private final Quantity count;
    private final Money amount;
    private OrderStatus status;

    public Order(OrderId id, UserId userId, ProductId productId, Quantity count, Money amount, OrderStatus status) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.count = count;
        this.amount = amount;
        this.status = status != null ? status : OrderStatus.INIT;
    }

    /**
     * 靜態工廠方法：建立新訂單
     *
     * @param id        訂單 ID (通常由雪花算法生成)
     * @param userId    用戶 ID
     * @param productId 商品 ID
     * @param count     數量
     * @param amount    金額
     * @return 訂單聚合根
     */
    public static Order create(OrderId id, UserId userId, ProductId productId, Quantity count, Money amount) {
        Order order = new Order(id, userId, productId, count, amount, OrderStatus.INIT);
        order.markAsSuccess();
        return order;
    }

    public void markAsSuccess() {
        this.status = OrderStatus.SUCCESS;
    }

    public void markAsFailed() {
        this.status = OrderStatus.FAILED;
    }

    public OrderCreatedEvent createEvent() {
        return new OrderCreatedEvent(
                this.id.getValue(),
                this.userId.getValue(),
                this.productId.getValue(),
                this.count.getValue(),
                this.amount.getValue()
        );
    }

    public OrderId getId() {
        return id;
    }

    public UserId getUserId() {
        return userId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public Quantity getCount() {
        return count;
    }

    public Money getAmount() {
        return amount;
    }

    public OrderStatus getStatus() {
        return status;
    }
}
