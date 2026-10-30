package org.example.transactionservice;

import org.example.transactionservice.service.TransactionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties(TransactionProperties.class)
public class TransactionServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner logTransactionCeiling(TransactionProperties properties) {
        return args -> log.info("Plafond chargé depuis la configuration : bank.transaction.max-amount={}",
                properties.getMaxAmount().toPlainString());
    }

}
