package org.example.transactionservice.client;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountSnapshot(
        UUID id,
        String customerReference,
        String status,
        String currency,
        BigDecimal balance
) {
}
