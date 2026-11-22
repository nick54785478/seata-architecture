package com.seata.demo.order.domain.order.exception;

/**
 * 查無訂單領域例外 (Order Not Found Domain Exception)
 *
 * @author Seata Architecture Demo Team
 */
public class OrderNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OrderNotFoundException(String message) {
        super(message);
    }

    public OrderNotFoundException(Long orderId) {
        super("查無此訂單記錄, OrderId: " + orderId);
    }
}
