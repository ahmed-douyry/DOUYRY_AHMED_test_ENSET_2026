package org.example.accountservice;

import org.example.accountservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void clean() {
        accountRepository.deleteAll();
    }

    @Test
    void createAndConsultAccount() throws Exception {
        String response = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerReference":"CUST-1","currency":"eur","balance":250.0}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerReference").value("CUST-1"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.balance").value(250.0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = response.replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/accounts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ accounts { customerReference currency status } }\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accounts[0].customerReference").value("CUST-1"))
                .andExpect(jsonPath("$.data.accounts[0].currency").value("EUR"));
    }

    @Test
    void debitReducesBalanceAndRejectsBlockedAccount() throws Exception {
        String response = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerReference":"CUST-2","currency":"MAD","balance":100}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id = response.replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/accounts/" + id + "/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":40}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(60.0));

        mockMvc.perform(post("/api/accounts/" + id + "/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":1000}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/accounts/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BLOCKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        mockMvc.perform(post("/api/accounts/" + id + "/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownAccountReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/accounts/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isNotFound());
    }
}
