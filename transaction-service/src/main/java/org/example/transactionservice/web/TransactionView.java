package org.example.transactionservice.web;

import org.example.transactionservice.domain.BankTransaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionView(
        UUID id,
        UUID accountId,
        BigDecimal amount,
        String currency,
        String merchantCategory,
        String country,
        Instant createdAt
) {
    public static TransactionView from(BankTransaction transaction) {
        return new TransactionView(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getMerchantCategory(),
                transaction.getCountry(),
                transaction.getCreatedAt()
        );
    }
}
