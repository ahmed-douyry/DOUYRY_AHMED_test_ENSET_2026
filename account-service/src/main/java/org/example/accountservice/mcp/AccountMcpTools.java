package org.example.accountservice.mcp;

import org.example.accountservice.domain.AccountStatus;
import org.example.accountservice.service.AccountService;
import org.example.accountservice.web.AccountView;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AccountMcpTools {

    private final AccountService accountService;

    public AccountMcpTools(AccountService accountService) {
        this.accountService = accountService;
    }

    @McpTool(description = "Liste les comptes bancaires")
    public List<AccountView> listAccounts() {
        return accountService.findAll().stream().map(AccountView::from).toList();
    }

    @McpTool(description = "Consulte un compte bancaire par son identifiant")
    public AccountView getAccount(
            @McpToolParam(description = "Identifiant UUID du compte", required = true) String accountId) {
        return AccountView.from(accountService.findById(UUID.fromString(accountId)));
    }

    @McpTool(description = "Crée un compte bancaire")
    public AccountView createAccount(
            @McpToolParam(description = "Référence client", required = true) String customerReference,
            @McpToolParam(description = "Code devise ISO sur 3 lettres", required = true) String currency,
            @McpToolParam(description = "Solde initial, 0 par défaut", required = false) Double balance) {
        return AccountView.from(accountService.create(customerReference, currency, balance, AccountStatus.ACTIVE));
    }
}
