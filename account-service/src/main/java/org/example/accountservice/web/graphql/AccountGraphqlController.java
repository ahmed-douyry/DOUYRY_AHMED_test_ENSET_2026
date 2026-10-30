package org.example.accountservice.web.graphql;

import org.example.accountservice.domain.AccountStatus;
import org.example.accountservice.service.AccountNotFoundException;
import org.example.accountservice.service.AccountService;
import org.example.accountservice.web.AccountView;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
public class AccountGraphqlController {

    private final AccountService accountService;

    public AccountGraphqlController(AccountService accountService) {
        this.accountService = accountService;
    }

    @QueryMapping
    public AccountView account(@Argument String id) {
        try {
            return AccountView.from(accountService.findById(UUID.fromString(id)));
        } catch (AccountNotFoundException | IllegalArgumentException exception) {
            return null;
        }
    }

    @QueryMapping
    public List<AccountView> accounts() {
        return accountService.findAll().stream().map(AccountView::from).toList();
    }

    @MutationMapping
    public AccountView createAccount(@Argument CreateAccountInput input) {
        return AccountView.from(accountService.create(
                input.customerReference(),
                input.currency(),
                input.balance(),
                input.status()
        ));
    }

    @MutationMapping
    public AccountView updateAccountStatus(@Argument String id, @Argument AccountStatus status) {
        return AccountView.from(accountService.updateStatus(UUID.fromString(id), status));
    }

    @MutationMapping
    public AccountView creditAccount(@Argument String id, @Argument double amount) {
        return AccountView.from(accountService.credit(UUID.fromString(id), amount));
    }

    @MutationMapping
    public AccountView debitAccount(@Argument String id, @Argument double amount) {
        return AccountView.from(accountService.debit(UUID.fromString(id), amount));
    }
}
