package org.example.transactionservice.support;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class AccountStubServer {

    public final AtomicInteger hits = new AtomicInteger();
    public final AtomicInteger debitHits = new AtomicInteger();
    public final Map<UUID, AccountState> accounts = new ConcurrentHashMap<>();
    public volatile int forcedStatus;

    private final HttpServer server;

    public AccountStubServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/accounts", exchange -> {
            hits.incrementAndGet();
            try {
                if (forcedStatus > 0) {
                    write(exchange, forcedStatus, "{\"error\":\"indisponible\"}");
                    return;
                }
                String[] parts = exchange.getRequestURI().getPath().split("/");
                if (parts.length < 4) {
                    write(exchange, 404, "{\"error\":\"Compte introuvable\"}");
                    return;
                }
                UUID id = UUID.fromString(parts[3]);
                AccountState state = accounts.get(id);
                if (state == null) {
                    exchange.getRequestBody().readAllBytes();
                    write(exchange, 404, "{\"error\":\"Compte introuvable\"}");
                    return;
                }
                if (parts.length == 5 && "POST".equals(exchange.getRequestMethod())) {
                    BigDecimal amount = readAmount(exchange);
                    if ("debit".equals(parts[4])) {
                        debitHits.incrementAndGet();
                        state.balance = state.balance.subtract(amount);
                    } else if ("credit".equals(parts[4])) {
                        state.balance = state.balance.add(amount);
                    }
                } else {
                    exchange.getRequestBody().readAllBytes();
                }
                write(exchange, 200, state.json());
            } catch (Exception exception) {
                write(exchange, 500, "{\"error\":\"stub\"}");
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void reset() {
        hits.set(0);
        debitHits.set(0);
        forcedStatus = 0;
        accounts.clear();
    }

    public AccountState add(String status, String currency, String balance) {
        AccountState state = new AccountState(UUID.randomUUID(), status, currency, new BigDecimal(balance));
        accounts.put(state.id, state);
        return state;
    }

    private static BigDecimal readAmount(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        int separator = json.indexOf(':');
        String number = json.substring(separator + 1).replaceAll("[^0-9.\\-]", "");
        return new BigDecimal(number);
    }

    private static void write(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    public static final class AccountState {
        public final UUID id;
        public final String status;
        public final String currency;
        public BigDecimal balance;

        AccountState(UUID id, String status, String currency, BigDecimal balance) {
            this.id = id;
            this.status = status;
            this.currency = currency;
            this.balance = balance;
        }

        String json() {
            return """
                    {"id":"%s","customerReference":"CUST","status":"%s","currency":"%s","balance":%s}
                    """.formatted(id, status, currency, balance.toPlainString());
        }
    }
}
