package org.example.transactionservice.client;

import org.example.transactionservice.service.AccountNotFoundException;
import org.example.transactionservice.service.AccountServiceUnavailableException;
import org.example.transactionservice.service.InvalidTransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AccountClientFallbackFactory implements FallbackFactory<AccountClient> {

    private static final Logger log = LoggerFactory.getLogger(AccountClientFallbackFactory.class);

    @Override
    public AccountClient create(Throwable cause) {
        return new AccountClient() {
            @Override
            public AccountSnapshot getAccount(UUID id) {
                throw translate(cause);
            }

            @Override
            public AccountSnapshot debit(UUID id, AmountPayload amount) {
                throw translate(cause);
            }

            @Override
            public AccountSnapshot credit(UUID id, AmountPayload amount) {
                throw translate(cause);
            }
        };
    }

    private RuntimeException translate(Throwable cause) {
        Throwable current = cause;
        while (current != null) {
            if (current instanceof InvalidTransactionException || current instanceof AccountNotFoundException) {
                return (RuntimeException) current;
            }
            current = current.getCause();
        }
        log.warn("Appel account-service en échec : {}", cause.toString());
        return new AccountServiceUnavailableException("Le service des comptes est indisponible. Réessayez plus tard.");
    }
}
