package org.example.transactionservice.client;

import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ResilientAccountGateway {

    private final AccountClient accountClient;

    public ResilientAccountGateway(AccountClient accountClient) {
        this.accountClient = accountClient;
    }

    @Retry(name = "accountServiceRead")
    public AccountSnapshot getAccount(UUID id) {
        return accountClient.getAccount(id);
    }

    public AccountSnapshot debit(UUID id, BigDecimal amount) {
        return accountClient.debit(id, new AmountPayload(amount));
    }

    public AccountSnapshot credit(UUID id, BigDecimal amount) {
        return accountClient.credit(id, new AmountPayload(amount));
    }
}
