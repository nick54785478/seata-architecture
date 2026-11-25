package com.seata.demo.gateway.config;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.WebFilter;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

/**
 * 網關微服務 OpenAPI 3.0 配置 (Gateway Service OpenAPI Configuration)
 * 位於 Clean Architecture 的 Configuration Layer，作為集中式 Swagger UI 入口，匯聚下游所有微服務之
 * API 規格。
 */
@Configuration
public class OpenApiConfig {

    /**
     * 註冊網關微服務 OpenAPI 規格 Bean
     *
     * @return OpenAPI 規格元數據物件
     */
    @Bean
    public OpenAPI gatewayServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("微服務聚合 API 網關 (API Gateway)")
                        .description("Spring Cloud Gateway 統一對外入口，聚合路由並提供全局 OpenAPI / Swagger 聚合文檔瀏覽。")
                        .version("1.0.0-SNAPSHOT")
                        .contact(new Contact()
                                .name("Seata Architecture Team")
                                .email("architecture@seata.demo.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }

    /**
     * 自動重定向 WebFilter
     * 當使用者訪問 /swagger-ui/index.html 或 /swagger-ui 時，自動 302 重定向至 WebFlux 標準路徑
     * /webjars/swagger-ui/index.html，避免白畫面。
     *
     * @return WebFilter
     */
    @Bean
    public WebFilter swaggerIndexRedirectFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            if ("/swagger-ui/index.html".equals(path) || "/swagger-ui".equals(path) || "/swagger-ui/".equals(path)) {
                exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                exchange.getResponse().getHeaders().setLocation(URI.create("/webjars/swagger-ui/index.html"));
                return exchange.getResponse().setComplete();
            }
            return chain.filter(exchange);
        };
    }
}
