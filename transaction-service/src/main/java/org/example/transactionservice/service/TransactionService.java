package org.example.transactionservice.service;

import org.example.transactionservice.client.AccountSnapshot;
import org.example.transactionservice.client.ResilientAccountGateway;
import org.example.transactionservice.domain.BankTransaction;
import org.example.transactionservice.repository.BankTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    static final String TRANSFER_OUT = "TRANSFER_OUT";
    static final String TRANSFER_IN = "TRANSFER_IN";

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final BankTransactionRepository transactionRepository;
    private final ResilientAccountGateway accountGateway;
    private final TransactionProperties properties;

    public TransactionService(BankTransactionRepository transactionRepository,
                              ResilientAccountGateway accountGateway,
                              TransactionProperties properties) {
        this.transactionRepository = transactionRepository;
        this.accountGateway = accountGateway;
        this.properties = properties;
    }

    @Transactional
    public BankTransaction create(UUID accountId, BigDecimal amount, String currency, String merchantCategory, String country) {
        BigDecimal normalizedAmount = normalizeAmount(amount);
        String normalizedCurrency = normalizeCurrency(currency);
        String category = requireText(merchantCategory, "La catégorie marchande est obligatoire.");
        String normalizedCountry = requireText(country, "Le pays est obligatoire.");

        AccountSnapshot account = loadAccount(accountId);
        ensureCanDebit(account, normalizedCurrency, normalizedAmount);

        BankTransaction transaction = newTransaction(accountId, normalizedAmount, normalizedCurrency, category, normalizedCountry);
        BankTransaction saved = transactionRepository.save(transaction);
        accountGateway.debit(accountId, normalizedAmount);
        return saved;
    }

    @Transactional
    public List<BankTransaction> transfer(UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount, String currency, String country) {
        if (sourceAccountId == null || destinationAccountId == null) {
            throw new InvalidTransactionException("Les deux comptes du virement sont obligatoires.");
        }
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new InvalidTransactionException("Le virement doit concerner deux comptes distincts.");
        }
        BigDecimal normalizedAmount = normalizeAmount(amount);
        String normalizedCurrency = normalizeCurrency(currency);
        String normalizedCountry = requireText(country, "Le pays est obligatoire.");

        AccountSnapshot source = loadAccount(sourceAccountId);
        AccountSnapshot destination = loadAccount(destinationAccountId);
        ensureCanDebit(source, normalizedCurrency, normalizedAmount);
        ensureActiveAndCurrency(destination, normalizedCurrency);

        BankTransaction outgoing = newTransaction(sourceAccountId, normalizedAmount, normalizedCurrency, TRANSFER_OUT, normalizedCountry);
        BankTransaction incoming = newTransaction(destinationAccountId, normalizedAmount, normalizedCurrency, TRANSFER_IN, normalizedCountry);
        BankTransaction savedOutgoing = transactionRepository.save(outgoing);
        BankTransaction savedIncoming = transactionRepository.save(incoming);

        accountGateway.debit(sourceAccountId, normalizedAmount);
        try {
            accountGateway.credit(destinationAccountId, normalizedAmount);
        } catch (RuntimeException exception) {
            compensateDebit(sourceAccountId, normalizedAmount, exception);
            throw exception;
        }
        return List.of(savedOutgoing, savedIncoming);
    }

    @Transactional(readOnly = true)
    public List<BankTransaction> findAll(UUID accountId) {
        if (accountId == null) {
            return transactionRepository.findAllByOrderByCreatedAtDesc();
        }
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    @Transactional(readOnly = true)
    public BankTransaction findById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new InvalidTransactionException("Transaction introuvable : " + id));
    }

    private AccountSnapshot loadAccount(UUID accountId) {
        if (accountId == null) {
            throw new InvalidTransactionException("Le compte est obligatoire.");
        }
        try {
            return accountGateway.getAccount(accountId);
        } catch (AccountNotFoundException exception) {
            throw new AccountNotFoundException(accountId);
        }
    }

    private void ensureCanDebit(AccountSnapshot account, String currency, BigDecimal amount) {
        ensureActiveAndCurrency(account, currency);
        if (account.balance() == null || account.balance().compareTo(amount) < 0) {
            throw new InvalidTransactionException("Solde insuffisant.");
        }
    }

    private void ensureActiveAndCurrency(AccountSnapshot account, String currency) {
        if (account.status() == null || !"ACTIVE".equalsIgnoreCase(account.status())) {
            throw new InvalidTransactionException("Le compte doit exister et être actif.");
        }
        if (account.currency() == null || !currency.equalsIgnoreCase(account.currency())) {
            throw new InvalidTransactionException("La devise de la transaction doit correspondre à celle du compte.");
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Le montant doit être strictement positif.");
        }
        if (amount.scale() > 2) {
            throw new InvalidTransactionException("Le montant ne peut pas avoir plus de deux décimales.");
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal maxAmount = properties.getMaxAmount();
        if (normalized.compareTo(maxAmount) > 0) {
            throw new InvalidTransactionException("Le montant dépasse le plafond autorisé (" + maxAmount.toPlainString() + ").");
        }
        return normalized;
    }

    private static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new InvalidTransactionException("La devise est obligatoire.");
        }
        String normalized = currency.trim().toUpperCase();
        if (!normalized.matches("[A-Z]{3}")) {
            throw new InvalidTransactionException("La devise doit être un code ISO de 3 lettres.");
        }
        return normalized;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidTransactionException(message);
        }
        return value.trim();
    }

    private static BankTransaction newTransaction(UUID accountId, BigDecimal amount, String currency, String merchantCategory, String country) {
        BankTransaction transaction = new BankTransaction();
        transaction.setId(UUID.randomUUID());
        transaction.setAccountId(accountId);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setMerchantCategory(merchantCategory);
        transaction.setCountry(country);
        transaction.setCreatedAt(Instant.now());
        return transaction;
    }

    private void compensateDebit(UUID accountId, BigDecimal amount, RuntimeException original) {
        try {
            accountGateway.credit(accountId, amount);
        } catch (RuntimeException compensation) {
            log.error("Compensation du débit impossible pour le compte {} après l'échec du crédit", accountId, compensation);
            original.addSuppressed(compensation);
        }
    }
}
