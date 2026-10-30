package org.example.accountservice.service;

import org.example.accountservice.domain.Account;
import org.example.accountservice.domain.AccountStatus;
import org.example.accountservice.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account create(String customerReference, String currency, Double balance, AccountStatus status) {
        return create(UUID.randomUUID(), customerReference, currency, balance, status);
    }

    public Account create(UUID id, String customerReference, String currency, Double balance, AccountStatus status) {
        if (customerReference == null || customerReference.isBlank()) {
            throw new InvalidAccountOperationException("La référence client est obligatoire.");
        }
        String normalizedCurrency = normalizeCurrency(currency);
        double initialBalance = balance == null ? 0d : balance;
        if (initialBalance < 0) {
            throw new InvalidAccountOperationException("Le solde initial ne peut pas être négatif.");
        }

        Account account = new Account();
        account.setId(id == null ? UUID.randomUUID() : id);
        account.setCustomerReference(customerReference.trim());
        account.setCurrency(normalizedCurrency);
        account.setBalance(initialBalance);
        account.setStatus(status == null ? AccountStatus.ACTIVE : status);
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Account findById(UUID id) {
        return accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    public Account updateStatus(UUID id, AccountStatus status) {
        if (status == null) {
            throw new InvalidAccountOperationException("Le statut du compte est obligatoire.");
        }
        Account account = findById(id);
        account.setStatus(status);
        return accountRepository.save(account);
    }

    public Account credit(UUID id, double amount) {
        requirePositiveAmount(amount);
        Account account = findById(id);
        account.setBalance(account.getBalance() + amount);
        return accountRepository.save(account);
    }

    public Account debit(UUID id, double amount) {
        requirePositiveAmount(amount);
        Account account = findById(id);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountOperationException("Le compte doit être actif.");
        }
        if (account.getBalance() < amount) {
            throw new InvalidAccountOperationException("Solde insuffisant.");
        }
        account.setBalance(account.getBalance() - amount);
        return accountRepository.save(account);
    }

    private static void requirePositiveAmount(double amount) {
        if (amount <= 0) {
            throw new InvalidAccountOperationException("Le montant doit être strictement positif.");
        }
    }

    private static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new InvalidAccountOperationException("La devise est obligatoire.");
        }
        String normalized = currency.trim().toUpperCase();
        if (!normalized.matches("[A-Z]{3}")) {
            throw new InvalidAccountOperationException("La devise doit être un code ISO de 3 lettres.");
        }
        return normalized;
    }
}
