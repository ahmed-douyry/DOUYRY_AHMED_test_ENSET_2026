package org.example.transactionservice;

import org.example.transactionservice.repository.BankTransactionRepository;
import org.example.transactionservice.support.AccountStubServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionApiTests {

    private static AccountStubServer accounts;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BankTransactionRepository transactionRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws IOException {
        accounts = new AccountStubServer();
        registry.add("bank.account-service.url", accounts::url);
    }

    @BeforeEach
    void clean() {
        transactionRepository.deleteAll();
        accounts.reset();
    }

    @Test
    void recordsTransactionAndDebitsAccount() throws Exception {
        AccountStubServer.AccountState account = accounts.add("ACTIVE", "EUR", "250.00");

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":"%s","amount":40.50,"currency":"eur","merchantCategory":"GROCERY","country":"FR"}
                                """.formatted(account.id)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.amount").value(40.50))
                .andExpect(jsonPath("$.merchantCategory").value("GROCERY"));

        org.junit.jupiter.api.Assertions.assertEquals(0, new java.math.BigDecimal("209.50").compareTo(account.balance));
        org.junit.jupiter.api.Assertions.assertEquals(1, accounts.debitHits.get());

        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ transactions { currency merchantCategory amount } }\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactions[0].currency").value("EUR"))
                .andExpect(jsonPath("$.data.transactions[0].amount").value("40.50"));
    }

    @Test
    void rejectsAmountAboveCeilingWithoutCallingAccountService() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":"00000000-0000-0000-0000-000000000001","amount":1000.01,"currency":"EUR","merchantCategory":"GROCERY","country":"FR"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("plafond")));

        org.junit.jupiter.api.Assertions.assertEquals(0, accounts.hits.get());
    }

    @Test
    void rejectsBlockedAccountAndCurrencyMismatch() throws Exception {
        AccountStubServer.AccountState blocked = accounts.add("BLOCKED", "EUR", "500");
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":"%s","amount":10,"currency":"EUR","merchantCategory":"GROCERY","country":"MA"}
                                """.formatted(blocked.id)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("actif")));
        org.junit.jupiter.api.Assertions.assertEquals(0, accounts.debitHits.get());

        AccountStubServer.AccountState foreign = accounts.add("ACTIVE", "MAD", "500");
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":"%s","amount":10,"currency":"EUR","merchantCategory":"GROCERY","country":"MA"}
                                """.formatted(foreign.id)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("devise")));
    }

    @Test
    void rejectsUnknownAccountAndInsufficientBalance() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":"00000000-0000-0000-0000-000000000099","amount":10,"currency":"EUR","merchantCategory":"GROCERY","country":"FR"}
                                """))
                .andExpect(status().isNotFound());

        AccountStubServer.AccountState account = accounts.add("ACTIVE", "EUR", "20.00");
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":"%s","amount":50,"currency":"EUR","merchantCategory":"GROCERY","country":"FR"}
                                """.formatted(account.id)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Solde")));
        org.junit.jupiter.api.Assertions.assertEquals(0, accounts.debitHits.get());
        org.junit.jupiter.api.Assertions.assertEquals(0, transactionRepository.count());
    }

    @Test
    void transfersBetweenTwoAccounts() throws Exception {
        AccountStubServer.AccountState source = accounts.add("ACTIVE", "EUR", "100.00");
        AccountStubServer.AccountState destination = accounts.add("ACTIVE", "EUR", "15.00");

        mockMvc.perform(post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceAccountId":"%s","destinationAccountId":"%s","amount":30,"currency":"EUR","country":"MA"}
                                """.formatted(source.id, destination.id)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].merchantCategory").value("TRANSFER_OUT"))
                .andExpect(jsonPath("$[0].accountId").value(source.id.toString()))
                .andExpect(jsonPath("$[1].merchantCategory").value("TRANSFER_IN"))
                .andExpect(jsonPath("$[1].accountId").value(destination.id.toString()));

        org.junit.jupiter.api.Assertions.assertEquals(0, new java.math.BigDecimal("70.00").compareTo(source.balance));
        org.junit.jupiter.api.Assertions.assertEquals(0, new java.math.BigDecimal("45.00").compareTo(destination.balance));
    }
}
