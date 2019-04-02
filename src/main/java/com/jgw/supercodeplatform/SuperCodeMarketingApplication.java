package com.jgw.supercodeplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableEurekaClient
@EnableAsync//允许异步
@EnableScheduling
@EnableTransactionManagement
public class SuperCodeMarketingApplication {


    public static void main(String[] args) {
    	
        SpringApplication.run(SuperCodeMarketingApplication.class, args);
    }

}
