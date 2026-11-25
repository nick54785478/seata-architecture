package com.seata.demo.shared.response;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 統一 API 回傳載體 (Unified API Response Wrapper)
 *
 * @param <T> 響應數據主體類型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 狀態碼 (200: 成功, 其他: 失敗)
     */
    private Integer code;

    /**
     * 響應提示訊息
     */
    private String message;

    /**
     * 響應數據
     */
    private T data;

    /**
     * 建構成功回應
     *
     * @param data 回傳數據
     * @param <T>  數據類型
     * @return 成功 Result 實例
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .message("SUCCESS")
                .data(data)
                .build();
    }

    /**
     * 建構無回傳資料之成功回應
     *
     * @return 成功 Result 實例
     */
    public static Result<Void> success() {
        return Result.<Void>builder()
                .code(200)
                .message("SUCCESS")
                .data(null)
                .build();
    }

    /**
     * 建構失敗回應
     *
     * @param code    錯誤碼
     * @param message 錯誤訊息
     * @param <T>     數據類型
     * @return 失敗 Result 實例
     */
    public static <T> Result<T> failure(Integer code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }

    /**
     * 建構預設 500 失敗回應
     *
     * @param message 錯誤訊息
     * @param <T>     數據類型
     * @return 失敗 Result 實例
     */
    public static <T> Result<T> failure(String message) {
        return failure(500, message);
    }
}
