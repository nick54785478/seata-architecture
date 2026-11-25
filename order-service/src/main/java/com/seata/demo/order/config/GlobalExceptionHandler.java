package com.seata.demo.order.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.seata.demo.order.domain.order.exception.OrderNotFoundException;
import com.seata.demo.shared.exception.BusinessException;
import com.seata.demo.shared.response.Result;
import feign.FeignException;

/**
 * 訂單服務全局例外處理器 (Order Global Exception Handler)
 * 位於 Configuration 層，統一攔截 Controller 與事務拋出之各類領域例外、業務狀態例外與模擬例外，
 * 確保在觸發 Seata 全局回滾後，能夠向客戶端輸出結構化且具備業務語意的 Result 響應。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 攔截找不到訂單例外 (OrderNotFoundException)
     */
    @ExceptionHandler(OrderNotFoundException.class)
    public Result<Void> handleOrderNotFoundException(OrderNotFoundException ex) {
        log.warn("[OrderService] 查無訂單業務例外: {}", ex.getMessage());
        return Result.failure(404, ex.getMessage());
    }

    /**
     * 攔截自定義業務例外 (BusinessException)
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex) {
        log.warn("[OrderService] 業務例外 (事務已回滾): code={}, message={}", ex.getCode(), ex.getMessage());
        return Result.failure(ex.getCode(), ex.getMessage());
    }

    /**
     * 攔截下游業務狀態例外 (如遠程庫存不足或帳戶餘額不足引發之中斷)
     */
    @ExceptionHandler(IllegalStateException.class)
    public Result<Void> handleIllegalStateException(IllegalStateException ex) {
        log.warn("[OrderService] 下游業務阻斷例外 (觸發全局回滾): {}", ex.getMessage());
        return Result.failure(400, ex.getMessage());
    }

    /**
     * 攔截遠程 Feign 調用異常 (如網路故障或連線中斷)
     */
    @ExceptionHandler(FeignException.class)
    public Result<Void> handleFeignException(FeignException ex) {
        log.error("[OrderService] 遠端 Feign 調用異常: status={}, message={}", ex.status(), ex.getMessage());
        return Result.failure(503, "下游微服務通訊異常: " + ex.getMessage());
    }

    /**
     * 攔截請求參數校驗例外
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<Void> handleValidationException(Exception ex) {
        String errorMsg = "參數驗證失敗";
        if (ex instanceof MethodArgumentNotValidException manv) {
            FieldError fieldError = manv.getBindingResult().getFieldError();
            if (fieldError != null) {
                errorMsg = fieldError.getField() + ": " + fieldError.getDefaultMessage();
            }
        } else if (ex instanceof BindException be) {
            FieldError fieldError = be.getBindingResult().getFieldError();
            if (fieldError != null) {
                errorMsg = fieldError.getField() + ": " + fieldError.getDefaultMessage();
            }
        }
        log.warn("[OrderService] 請求參數驗證失敗: {}", errorMsg);
        return Result.failure(400, errorMsg);
    }

    /**
     * 攔截非法參數例外
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("[OrderService] 參數不合法例外: {}", ex.getMessage());
        return Result.failure(400, ex.getMessage());
    }

    /**
     * 攔截業務運行時例外 (包含 Seata @GlobalTransactional 切面與 simulateError 觸發的例外)
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException ex) {
        Throwable rootCause = getRootCause(ex);
        if (rootCause instanceof OrderNotFoundException onfe) {
            return handleOrderNotFoundException(onfe);
        }
        if (rootCause instanceof IllegalStateException ise) {
            return handleIllegalStateException(ise);
        }
        if (rootCause instanceof BusinessException be) {
            return handleBusinessException(be);
        }
        log.warn("[OrderService] 業務運行時例外 (觸發全局回滾): {}", ex.getMessage());
        String msg = rootCause != null && rootCause.getMessage() != null ? rootCause.getMessage() : ex.getMessage();
        return Result.failure(500, msg);
    }

    /**
     * 攔截全局未預期內部例外
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleGenericException(Exception ex) {
        Throwable rootCause = getRootCause(ex);
        if (rootCause instanceof IllegalStateException ise) {
            return handleIllegalStateException(ise);
        }
        log.error("[OrderService] 系統未預期內部例外", ex);
        String msg = rootCause != null && rootCause.getMessage() != null ? rootCause.getMessage() : ex.getMessage();
        return Result.failure(500, "訂單服務內部異常: " + msg);
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}
