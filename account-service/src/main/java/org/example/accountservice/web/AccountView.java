package org.example.accountservice.web;

import org.example.accountservice.domain.Account;
import org.example.accountservice.domain.AccountStatus;

import java.util.UUID;

public record AccountView(
        UUID id,
        String customerReference,
        AccountStatus status,
        String currency,
        double balance
) {
    public static AccountView from(Account account) {
        return new AccountView(
                account.getId(),
                account.getCustomerReference(),
                account.getStatus(),
                account.getCurrency(),
                account.getBalance()
        );
    }
}
