package org.example.aiagentservice.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class BankingAiAgent {

    static final String OUT_OF_SCOPE =
            "Je ne sais pas. Je suis spécialisé uniquement dans la gestion des comptes et des transactions bancaires.";

    private final ChatClient chatClient;

    public BankingAiAgent(
            ChatClient.Builder builder,
            ChatMemory chatMemory,
            ObjectProvider<ToolCallbackProvider> tools) {
        builder.defaultSystem("""
                        Tu es un assistant bancaire. Tu traites uniquement les comptes et les transactions de cette banque.

                        OUTILS DISPONIBLES, via les serveurs MCP :
                        - listAccounts, getAccount, createAccount
                        - listTransactions, getTransaction, createTransaction, transfer

                        COMPTES DE DÉMONSTRATION :
                        - USER-ALICE EUR : 11111111-1111-1111-1111-111111111111
                        - USER-ALICE MAD : 22222222-2222-2222-2222-222222222222
                        - USER-YOUSSEF EUR : 33333333-3333-3333-3333-333333333333
                        - USER-SARA MAD : 44444444-4444-4444-4444-444444444444
                        - USER-KARIM EUR, compte bloqué : 55555555-5555-5555-5555-555555555555

                        RÈGLES :
                        1. Réponds en français, uniquement sur les comptes, les soldes, les transactions et les virements.
                        2. Si la demande est hors domaine, réponds exactement : %s
                        3. N'invente jamais un compte, un solde ou une transaction. Utilise les outils.
                        4. Le plafond d'une transaction est de 1000. Le montant est une chaîne avec au plus deux décimales.
                        5. Avant de créer un compte, d'enregistrer une transaction ou de faire un virement, demande une confirmation explicite.
                        6. Si un outil échoue, explique l'erreur sans inventer un résultat.
                        """.formatted(OUT_OF_SCOPE))
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build());
        tools.ifAvailable(builder::defaultTools);
        this.chatClient = builder.build();
    }

    public String chat(String query, String conversationId) {
        Assert.hasText(query, "query cannot be null or empty");
        return chat(new Prompt(query), conversationId);
    }

    public String chat(Prompt prompt, String conversationId) {
        Assert.notNull(prompt, "prompt cannot be null");
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        String answer = chatClient.prompt(prompt)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
        return answer == null || answer.isBlank() ? "Je n'ai pas de réponse." : answer;
    }
}
