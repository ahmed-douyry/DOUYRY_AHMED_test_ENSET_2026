package org.example.aiagentservice.web;

import jakarta.servlet.http.HttpSession;
import org.example.aiagentservice.agent.BankingAiAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatRestController {

    private static final Logger log = LoggerFactory.getLogger(ChatRestController.class);

    private final BankingAiAgent agent;

    public ChatRestController(BankingAiAgent agent) {
        this.agent = agent;
    }

    @GetMapping("/chat")
    public ResponseEntity<String> chat(
            @RequestParam("query") String query,
            @RequestParam(value = "conversationId", required = false) String conversationId,
            HttpSession session) {
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().body("La question est vide.");
        }
        return ResponseEntity.ok(agent.chat(query, conversationId(conversationId, session)));
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request, HttpSession session) {
        if (request == null || request.query() == null || request.query().isBlank()) {
            return ResponseEntity.badRequest().body(new ChatResponse(null, "La question est vide."));
        }
        String conversationId = conversationId(request.conversationId(), session);
        return ResponseEntity.ok(new ChatResponse(conversationId, agent.chat(request.query(), conversationId)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ChatResponse> unavailable(Exception exception) {
        log.warn("Appel de l'assistant en échec : {}", exception.toString());
        return ResponseEntity.status(503).body(new ChatResponse(null, "L'assistant est indisponible."));
    }

    private static String conversationId(String requested, HttpSession session) {
        return requested == null || requested.isBlank() ? session.getId() : requested;
    }

    public record ChatRequest(String query, String conversationId) {
    }

    public record ChatResponse(String conversationId, String answer) {
    }
}
