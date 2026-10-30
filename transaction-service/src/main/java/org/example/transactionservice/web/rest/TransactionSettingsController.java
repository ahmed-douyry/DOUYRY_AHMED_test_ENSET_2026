package org.example.transactionservice.web.rest;

import org.example.transactionservice.service.TransactionProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class TransactionSettingsController {

    private final TransactionProperties properties;

    public TransactionSettingsController(TransactionProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public Map<String, String> maxAmount() {
        return Map.of("bank.transaction.max-amount", properties.getMaxAmount().toPlainString());
    }
}
