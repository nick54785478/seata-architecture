package com.seata.demo.shared.exception;

/**
 * 通用業務例外 (Base Business Exception)
 * 位於 Shared Module，提供跨微服務標準化之業務錯誤代碼與訊息載體。
 *
 * @author Seata Architecture Demo Team
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 業務錯誤碼 (例如: 400, 404, 500)
     */
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
