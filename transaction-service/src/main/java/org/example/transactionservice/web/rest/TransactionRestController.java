package org.example.transactionservice.web.rest;

import jakarta.validation.Valid;
import org.example.transactionservice.domain.BankTransaction;
import org.example.transactionservice.service.TransactionService;
import org.example.transactionservice.web.TransactionView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class TransactionRestController {

    private final TransactionService transactionService;

    public TransactionRestController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionView> create(@Valid @RequestBody CreateTransactionRequest request) {
        BankTransaction transaction = transactionService.create(
                request.accountId(),
                request.amount(),
                request.currency(),
                request.merchantCategory(),
                request.country()
        );
        return ResponseEntity.created(URI.create("/api/transactions/" + transaction.getId()))
                .body(TransactionView.from(transaction));
    }

    @GetMapping("/transactions")
    public List<TransactionView> list(@RequestParam(required = false) UUID accountId) {
        return transactionService.findAll(accountId).stream().map(TransactionView::from).toList();
    }

    @GetMapping("/transactions/{id}")
    public TransactionView get(@PathVariable UUID id) {
        return TransactionView.from(transactionService.findById(id));
    }

    @PostMapping("/transfers")
    public ResponseEntity<List<TransactionView>> transfer(@Valid @RequestBody TransferRequest request) {
        List<TransactionView> transfers = transactionService.transfer(
                request.sourceAccountId(),
                request.destinationAccountId(),
                request.amount(),
                request.currency(),
                request.country()
        ).stream().map(TransactionView::from).toList();
        return ResponseEntity.status(201).body(transfers);
    }
}
