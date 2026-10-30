package org.example.accountservice.web.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.example.accountservice.domain.AccountStatus;

public record CreateAccountRequest(
        @NotBlank String customerReference,
        @NotBlank String currency,
        @PositiveOrZero Double balance,
        AccountStatus status
) {
}
