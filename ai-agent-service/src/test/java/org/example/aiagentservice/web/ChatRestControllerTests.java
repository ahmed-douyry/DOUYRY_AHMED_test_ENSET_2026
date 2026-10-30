package org.example.aiagentservice.web;

import org.example.aiagentservice.agent.BankingAiAgent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatRestControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BankingAiAgent agent;

    @Test
    void answersGetChat() throws Exception {
        when(agent.chat(eq("liste les comptes"), anyString())).thenReturn("Deux comptes Alice.");

        mockMvc.perform(get("/chat").param("query", "liste les comptes"))
                .andExpect(status().isOk())
                .andExpect(content().string("Deux comptes Alice."));
    }

    @Test
    void answersPostChat() throws Exception {
        when(agent.chat(eq("solde"), eq("demo"))).thenReturn("2500 EUR");

        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"solde\",\"conversationId\":\"demo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value("demo"))
                .andExpect(jsonPath("$.answer").value("2500 EUR"));
    }

    @Test
    void rejectsEmptyQuestion() throws Exception {
        mockMvc.perform(get("/chat").param("query", " "))
                .andExpect(status().isBadRequest());
    }
}
