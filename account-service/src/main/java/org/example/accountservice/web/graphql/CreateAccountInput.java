package org.example.accountservice.web.graphql;

import org.example.accountservice.domain.AccountStatus;

public record CreateAccountInput(
        String customerReference,
        String currency,
        Double balance,
        AccountStatus status
) {
}
