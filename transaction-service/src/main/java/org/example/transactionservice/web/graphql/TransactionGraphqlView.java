package org.example.transactionservice.web.graphql;

import org.example.transactionservice.domain.BankTransaction;

import java.util.UUID;

public record TransactionGraphqlView(
        UUID id,
        UUID accountId,
        String amount,
        String currency,
        String merchantCategory,
        String country,
        String createdAt
) {
    public static TransactionGraphqlView from(BankTransaction transaction) {
        return new TransactionGraphqlView(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getAmount().toPlainString(),
                transaction.getCurrency(),
                transaction.getMerchantCategory(),
                transaction.getCountry(),
                transaction.getCreatedAt().toString()
        );
    }
}
