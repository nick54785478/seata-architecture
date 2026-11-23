package com.seata.demo.account.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.seata.demo.account.domain.account.exception.BalanceInsufficientException;
import com.seata.demo.shared.exception.BusinessException;
import com.seata.demo.shared.response.Result;

/**
 * 帳戶服務全局例外處理器 (Account Global Exception Handler)
 * 位於 Configuration 層，統一攔截 Controller 拋出之領域例外與系統例外，轉換為標準 Result 響應。
 *
 * @author Seata Architecture Demo Team
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 攔截查無用戶帳戶領域例外 (AccountNotFoundException)
     */
    @ExceptionHandler(com.seata.demo.account.domain.account.exception.AccountNotFoundException.class)
    public Result<Void> handleAccountNotFoundException(com.seata.demo.account.domain.account.exception.AccountNotFoundException ex) {
        log.warn("[AccountService] 查無帳戶業務例外: {}", ex.getMessage());
        return Result.failure(404, ex.getMessage());
    }

    /**
     * 攔截餘額不足領域例外 (BalanceInsufficientException)
     */
    @ExceptionHandler(BalanceInsufficientException.class)
    public Result<Void> handleBalanceInsufficientException(BalanceInsufficientException ex) {
        log.warn("[AccountService] 帳戶餘額不足業務例外: {}", ex.getMessage());
        return Result.failure(400, ex.getMessage());
    }

    /**
     * 攔截自定義業務例外
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException ex) {
        log.warn("[AccountService] 業務例外: code={}, message={}", ex.getCode(), ex.getMessage());
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
        log.warn("[AccountService] 請求參數驗證失敗: {}", errorMsg);
        return Result.failure(400, errorMsg);
    }

    /**
     * 攔截非法參數例外
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("[AccountService] 參數不合法例外: {}", ex.getMessage());
        return Result.failure(400, ex.getMessage());
    }

    /**
     * 攔截運行時例外 (含 Seata TCC 包裝例外)
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException ex) {
        Throwable rootCause = getRootCause(ex);
        if (rootCause instanceof BalanceInsufficientException bie) {
            return handleBalanceInsufficientException(bie);
        }
        if (rootCause instanceof BusinessException be) {
            return handleBusinessException(be);
        }
        log.error("[AccountService] 運行時未預期內部例外", ex);
        String msg = rootCause != null && rootCause.getMessage() != null ? rootCause.getMessage() : ex.getMessage();
        return Result.failure(500, "帳戶服務內部異常: " + msg);
    }

    /**
     * 攔截全局未處理例外與嚴重錯誤 (含 Error 與 Throwable)
     */
    @ExceptionHandler(Throwable.class)
    public Result<Void> handleGenericThrowable(Throwable ex) {
        Throwable rootCause = getRootCause(ex);
        if (rootCause instanceof BalanceInsufficientException bie) {
            return handleBalanceInsufficientException(bie);
        }
        log.error("[AccountService] 系統未預期內部例外", ex);
        String msg = rootCause != null && rootCause.getMessage() != null ? rootCause.getMessage() : ex.getMessage();
        return Result.failure(500, "帳戶服務內部異常: " + msg);
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}
