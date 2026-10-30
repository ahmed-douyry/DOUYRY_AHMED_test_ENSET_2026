package org.example.accountservice.web.rest;

import jakarta.validation.Valid;
import org.example.accountservice.domain.Account;
import org.example.accountservice.service.AccountService;
import org.example.accountservice.web.AccountView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountRestController {

    private final AccountService accountService;

    public AccountRestController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountView> create(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.create(
                request.customerReference(),
                request.currency(),
                request.balance(),
                request.status()
        );
        return ResponseEntity.created(URI.create("/api/accounts/" + account.getId()))
                .body(AccountView.from(account));
    }

    @GetMapping
    public List<AccountView> list() {
        return accountService.findAll().stream().map(AccountView::from).toList();
    }

    @GetMapping("/{id}")
    public AccountView get(@PathVariable UUID id) {
        return AccountView.from(accountService.findById(id));
    }

    @PatchMapping("/{id}/status")
    public AccountView updateStatus(@PathVariable UUID id, @Valid @RequestBody StatusRequest request) {
        return AccountView.from(accountService.updateStatus(id, request.status()));
    }

    @PostMapping("/{id}/credit")
    public AccountView credit(@PathVariable UUID id, @Valid @RequestBody AmountRequest request) {
        return AccountView.from(accountService.credit(id, request.amount()));
    }

    @PostMapping("/{id}/debit")
    public AccountView debit(@PathVariable UUID id, @Valid @RequestBody AmountRequest request) {
        return AccountView.from(accountService.debit(id, request.amount()));
    }
}
