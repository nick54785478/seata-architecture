package com.seata.demo.storage.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.seata.demo.shared.exception.BusinessException;
import com.seata.demo.shared.response.Result;
import com.seata.demo.storage.domain.storage.exception.StockInsufficientException;

/**
 * 庫存服務全局例外處理器 (Storage Global Exception Handler)
 * 位於 Configuration 層，統一攔截 Controller 拋出之領域例外與系統例外，轉換為標準 Result 響應。
 *
 * @author Seata Architecture Demo Team
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 攔截查無商品庫存領域例外 (StorageNotFoundException)
     */
    @ExceptionHandler(com.seata.demo.storage.domain.storage.exception.StorageNotFoundException.class)
    public Result<Void> handleStorageNotFoundException(com.seata.demo.storage.domain.storage.exception.StorageNotFoundException ex) {
        log.warn("[StorageService] 查無庫存業務例外: {}", ex.getMessage());
        return Result.failure(404, ex.getMessage());
    }

    /**
     * 攔截庫存不足領域例外 (StockInsufficientException)
     */
    @ExceptionHandler(StockInsufficientException.class)
    public Result<Void> handleStockInsufficientException(StockInsufficientException ex) {
        log.warn("[StorageService] 庫存不足業務例外: {}", ex.getMessage());
        return Result.failure(400, ex.getMessage());
    }

    /**
     * 攔截自定義業務例外
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex) {
        log.warn("[StorageService] 業務例外: code={}, message={}", ex.getCode(), ex.getMessage());
        return Result.failure(ex.getCode(), ex.getMessage());
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
        log.warn("[StorageService] 請求參數驗證失敗: {}", errorMsg);
        return Result.failure(400, errorMsg);
    }

    /**
     * 攔截非法參數例外
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("[StorageService] 參數不合法例外: {}", ex.getMessage());
        return Result.failure(400, ex.getMessage());
    }

    /**
     * 攔截全局未處理例外與嚴重錯誤 (含 Error 與 Throwable)
     */
    @ExceptionHandler(Throwable.class)
    public Result<Void> handleGenericThrowable(Throwable ex) {
        log.error("[StorageService] 系統未預期內部例外", ex);
        return Result.failure(500, "庫存服務內部異常: " + ex.getMessage());
    }
}
