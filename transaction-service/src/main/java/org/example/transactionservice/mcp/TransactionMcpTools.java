package org.example.transactionservice.mcp;

import org.example.transactionservice.service.TransactionService;
import org.example.transactionservice.web.TransactionView;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionMcpTools {

    private final TransactionService transactionService;

    public TransactionMcpTools(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @McpTool(description = "Liste les transactions, éventuellement filtrées par compte")
    public List<TransactionView> listTransactions(
            @McpToolParam(description = "Identifiant UUID du compte, optionnel", required = false) String accountId) {
        UUID filter = accountId == null || accountId.isBlank() ? null : UUID.fromString(accountId);
        return transactionService.findAll(filter).stream().map(TransactionView::from).toList();
    }

    @McpTool(description = "Consulte une transaction par son identifiant")
    public TransactionView getTransaction(
            @McpToolParam(description = "Identifiant UUID de la transaction", required = true) String transactionId) {
        return TransactionView.from(transactionService.findById(UUID.fromString(transactionId)));
    }

    @McpTool(description = "Enregistre une transaction et débite le compte")
    public TransactionView createTransaction(
            @McpToolParam(description = "Identifiant UUID du compte", required = true) String accountId,
            @McpToolParam(description = "Montant strictement positif, deux décimales maximum", required = true) String amount,
            @McpToolParam(description = "Code devise ISO sur 3 lettres", required = true) String currency,
            @McpToolParam(description = "Catégorie marchande", required = true) String merchantCategory,
            @McpToolParam(description = "Pays", required = true) String country) {
        return TransactionView.from(transactionService.create(
                UUID.fromString(accountId),
                new BigDecimal(amount),
                currency,
                merchantCategory,
                country
        ));
    }

    @McpTool(description = "Effectue un virement d'un compte vers un autre")
    public List<TransactionView> transfer(
            @McpToolParam(description = "Compte source", required = true) String sourceAccountId,
            @McpToolParam(description = "Compte destinataire", required = true) String destinationAccountId,
            @McpToolParam(description = "Montant du virement", required = true) String amount,
            @McpToolParam(description = "Code devise ISO sur 3 lettres", required = true) String currency,
            @McpToolParam(description = "Pays", required = true) String country) {
        return transactionService.transfer(
                UUID.fromString(sourceAccountId),
                UUID.fromString(destinationAccountId),
                new BigDecimal(amount),
                currency,
                country
        ).stream().map(TransactionView::from).toList();
    }
}
