package org.example.transactionservice.web.rest;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.example.transactionservice.service.AccountNotFoundException;
import org.example.transactionservice.service.AccountServiceUnavailableException;
import org.example.transactionservice.service.InvalidTransactionException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Map<String, String>> notFound(AccountNotFoundException exception) {
        return ResponseEntity.status(404).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<Map<String, String>> invalid(InvalidTransactionException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler({AccountServiceUnavailableException.class, FeignException.class, CallNotPermittedException.class})
    public ResponseEntity<Map<String, String>> unavailable(Exception exception) {
        String message = exception.getMessage() == null || exception.getMessage().isBlank()
                ? "Le service des comptes est indisponible. Réessayez plus tard."
                : exception.getMessage();
        return ResponseEntity.status(503).body(Map.of("error", message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " : " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> typeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", "Paramètre invalide : " + exception.getName()));
    }
}
