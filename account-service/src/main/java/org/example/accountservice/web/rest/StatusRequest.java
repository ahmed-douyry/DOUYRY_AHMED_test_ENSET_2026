package org.example.accountservice.web.rest;

import jakarta.validation.constraints.NotNull;
import org.example.accountservice.domain.AccountStatus;

public record StatusRequest(@NotNull AccountStatus status) {
}
