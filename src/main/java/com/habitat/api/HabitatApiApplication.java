package com.habitat.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

@SpringBootApplication
@EnableTransactionManagement
@EnableCaching
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class HabitatApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(HabitatApiApplication.class, args);
    }
}
