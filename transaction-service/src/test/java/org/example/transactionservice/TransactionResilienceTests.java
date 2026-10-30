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
@TestPropertySource(properties = {
        "resilience4j.retry.instances.accountServiceRead.max-attempts=1",
        "resilience4j.circuitbreaker.configs.account-service.sliding-window-size=2",
        "resilience4j.circuitbreaker.configs.account-service.minimum-number-of-calls=2",
        "resilience4j.circuitbreaker.configs.account-service.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.configs.account-service.wait-duration-in-open-state=30s"
})
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
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(io.github.resilience4j.circuitbreaker.CircuitBreaker::reset);
    }

    @Test
    void opensCircuitWhenAccountServiceFails() throws Exception {
        accounts.forcedStatus = 500;
        String body = """
                {"accountId":"00000000-0000-0000-0000-000000000001","amount":10,"currency":"EUR","merchantCategory":"GROCERY","country":"FR"}
                """;

        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error", containsString("indisponible")));
        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isServiceUnavailable());

        int hitsBeforeOpenCircuit = accounts.hits.get();
        mockMvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isServiceUnavailable());

        assertEquals(hitsBeforeOpenCircuit, accounts.hits.get());
        io.github.resilience4j.circuitbreaker.CircuitBreaker accountLookup = circuitBreakerRegistry.getAllCircuitBreakers()
                .stream()
                .filter(breaker -> breaker.getName().contains("getAccount"))
                .findFirst()
                .orElseThrow();
        assertEquals(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN, accountLookup.getState());
    }
}
