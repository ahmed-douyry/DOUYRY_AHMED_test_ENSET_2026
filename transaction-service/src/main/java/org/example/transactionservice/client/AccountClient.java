package org.example.transactionservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(
        name = "account-service",
        url = "${bank.account-service.url:}",
        fallbackFactory = AccountClientFallbackFactory.class
)
public interface AccountClient {

    @GetMapping("/api/accounts/{id}")
    AccountSnapshot getAccount(@PathVariable("id") UUID id);

    @PostMapping("/api/accounts/{id}/debit")
    AccountSnapshot debit(@PathVariable("id") UUID id, @RequestBody AmountPayload amount);

    @PostMapping("/api/accounts/{id}/credit")
    AccountSnapshot credit(@PathVariable("id") UUID id, @RequestBody AmountPayload amount);
}
