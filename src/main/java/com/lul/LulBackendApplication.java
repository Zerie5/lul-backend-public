package com.lul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.lul.config.SendGridConfig;

@SpringBootApplication
@EntityScan("com.lul.entity")
@EnableJpaRepositories("com.lul.repository")
@EnableConfigurationProperties(SendGridConfig.class)
@EnableScheduling
public class LulBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(LulBackendApplication.class, args);
    }
}