package com.sankalp.expensetracker.expense;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing
@EnableScheduling
@ComponentScan(basePackages = {
        "com.sankalp.expensetracker.expense",
        "com.sankalp.expensetracker.common"
})
public class ExpenseServiceApplication {
    public static void main(String[] args) { SpringApplication.run(ExpenseServiceApplication.class, args); }
}
