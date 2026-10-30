package org.example.transactionservice.client;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfiguration {

    @Bean
    public ErrorDecoder accountErrorDecoder() {
        return new AccountErrorDecoder();
    }
}
