package org.example.transactionservice;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.example.transactionservice.support.AccountStubServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "resilience4j.retry.instances.accountServiceRead.max-attempts=1")
class TransactionResilienceTests {

    private static AccountStubServer accounts;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws IOException {
        accounts = new AccountStubServer();
        registry.add("bank.account-service.url", accounts::url);
    }

    @BeforeEach
    void clean() {
        accounts.reset();
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(breaker -> circuitBreakerRegistry.remove(breaker.getName()));
    }

    @Test
    void opensCircuitWhenAccountServiceFails() throws Exception {
        accounts.forcedStatus = 500;
        String body = """
                {"accountId":"00000000-0000-0000-0000-000000000001","amount":10,"currency":"EUR","merchantCategory":"GROCERY","country":"FR"}
                """;

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error", containsString("indisponible")));
        }

        int hitsWhenOpen = accounts.hits.get();
        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isServiceUnavailable());
        assertEquals(hitsWhenOpen, accounts.hits.get(), "le disjoncteur doit couper les appels suivants");
    }
}
