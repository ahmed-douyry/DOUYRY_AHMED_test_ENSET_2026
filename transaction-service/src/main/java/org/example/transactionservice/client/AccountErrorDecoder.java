package org.example.transactionservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.example.transactionservice.service.AccountNotFoundException;
import org.example.transactionservice.service.AccountServiceUnavailableException;
import org.example.transactionservice.service.InvalidTransactionException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class AccountErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        String message = readMessage(response);
        if (response.status() == 404) {
            return new AccountNotFoundException(blankTo(message, "Compte introuvable."));
        }
        if (response.status() >= 400 && response.status() < 500) {
            return new InvalidTransactionException(blankTo(message, "Opération refusée par le service des comptes."));
        }
        return new AccountServiceUnavailableException("Le service des comptes est indisponible.");
    }

    private String readMessage(Response response) {
        if (response.body() == null) {
            return "";
        }
        try (InputStream stream = response.body().asInputStream()) {
            String body = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            if (body.isBlank()) {
                return "";
            }
            JsonNode node = objectMapper.readTree(body);
            if (node.hasNonNull("error")) {
                return node.get("error").asText();
            }
            return body;
        } catch (Exception exception) {
            return "";
        }
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
