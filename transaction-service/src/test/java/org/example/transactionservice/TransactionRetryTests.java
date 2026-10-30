package org.example.transactionservice;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionRetryTests {

    private static AccountStubServer accounts;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws IOException {
        accounts = new AccountStubServer();
        registry.add("bank.account-service.url", accounts::url);
    }

    @BeforeEach
    void clean() {
        accounts.reset();
    }

    @Test
    void retriesAccountLookupBeforeGivingUp() throws Exception {
        accounts.forcedStatus = 500;
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"accountId":"00000000-0000-0000-0000-000000000001","amount":10,"currency":"EUR","merchantCategory":"GROCERY","country":"FR"}
                                """))
                .andExpect(status().isServiceUnavailable());

        assertEquals(3, accounts.hits.get());
    }
}
