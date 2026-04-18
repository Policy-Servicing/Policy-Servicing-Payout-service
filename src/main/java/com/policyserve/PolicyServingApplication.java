package com.policyserve;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PolicyServingApplication {
    public static void main(String[] args) {
        SpringApplication.run(PolicyServingApplication.class, args);
    }
}
