package com.seata.demo.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 訂單微服務 CORS 跨來源資源共用配置 (Order Service CORS Configuration)
 * 位於 Clean Architecture 的 Configuration Layer (最外層技術配置)。
 * 配置全域 CorsFilter，解決前後端分離與 Swagger UI 跨來源呼叫問題。
 */
@Configuration
public class CorsConfig {

    /**
     * 註冊跨域過濾器 Bean，並設置最高優先順序以確保在安全或業務攔截器之前處理 OPTIONS 預檢請求
     *
     * @return CorsFilter 實例
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
