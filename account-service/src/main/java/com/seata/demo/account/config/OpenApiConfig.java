package com.seata.demo.account.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

/**
 * 帳戶微服務 OpenAPI 3.0 配置 (Account Service OpenAPI Configuration)
 * 位於 Clean Architecture 的 Configuration Layer (最外層技術配置)，集中管理 Swagger
 * 文檔元數據與規格定義。
 */
@Configuration
public class OpenApiConfig {

        /**
         * 註冊帳戶微服務 OpenAPI 規格 Bean
         *
         * @return OpenAPI 規格元數據物件
         */
        @Bean
        public OpenAPI accountServiceOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("帳戶微服務 API (Account Service)")
                                                .description("提供資金預留與結算功能，作為 Seata TCC 模式資源管理器 (RM)，實作兩階段自定義 Try-Confirm-Cancel，具備防懸掛、防空回滾與冪等性保障機制。")
                                                .version("1.0.0-SNAPSHOT")
                                                .contact(new Contact()
                                                                .name("Seata Architecture Team")
                                                                .email("architecture@seata.demo.com"))
                                                .license(new License()
                                                                .name("Apache 2.0")
                                                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                                .servers(List.of(new Server().url("/").description("相對路徑伺服器端點 (自動適應 Gateway 與直接訪問)")));
        }
}
