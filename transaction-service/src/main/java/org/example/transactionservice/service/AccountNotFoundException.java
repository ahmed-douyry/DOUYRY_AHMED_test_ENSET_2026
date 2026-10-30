package org.example.transactionservice.service;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(UUID id) {
        super("Compte introuvable : " + id);
    }

    public AccountNotFoundException(String message) {
        super(message);
    }
}
