package com.seata.demo.storage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 庫存微服務啟動主類別 (Storage Service Application)
 *
 * @author Seata Architecture Demo Team
 */
@SpringBootApplication
@EnableDiscoveryClient
public class StorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(StorageApplication.class, args);
    }
}
