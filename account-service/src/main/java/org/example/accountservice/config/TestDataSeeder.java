package org.example.accountservice.config;

import org.example.accountservice.domain.Account;
import org.example.accountservice.domain.AccountStatus;
import org.example.accountservice.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class TestDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(TestDataSeeder.class);

    @Bean
    @ConditionalOnProperty(name = "bank.seed.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner seedTestUsersAndAccounts(AccountService accountService) {
        return args -> {
            if (!accountService.findAll().isEmpty()) {
                log.info("Des comptes existent déjà, aucun jeu de test n'est inséré.");
                return;
            }

            Account aliceEur = account(accountService, "11111111-1111-1111-1111-111111111111", "USER-ALICE", "EUR", 2500d, AccountStatus.ACTIVE);
            Account aliceMad = account(accountService, "22222222-2222-2222-2222-222222222222", "USER-ALICE", "MAD", 8000d, AccountStatus.ACTIVE);
            Account youssef = account(accountService, "33333333-3333-3333-3333-333333333333", "USER-YOUSSEF", "EUR", 1500d, AccountStatus.ACTIVE);
            Account sara = account(accountService, "44444444-4444-4444-4444-444444444444", "USER-SARA", "MAD", 4200.5d, AccountStatus.ACTIVE);
            Account karim = account(accountService, "55555555-5555-5555-5555-555555555555", "USER-KARIM", "EUR", 300d, AccountStatus.BLOCKED);

            log.info("Utilisateurs et comptes de test chargés.");
            for (Account account : new Account[] {aliceEur, aliceMad, youssef, sara, karim}) {
                log.info("utilisateur={} compte={} devise={} solde={} statut={}",
                        account.getCustomerReference(),
                        account.getId(),
                        account.getCurrency(),
                        account.getBalance(),
                        account.getStatus());
            }
        };
    }

    private static Account account(AccountService accountService, String id, String user, String currency, double balance, AccountStatus status) {
        return accountService.create(UUID.fromString(id), user, currency, balance, status);
    }
}
