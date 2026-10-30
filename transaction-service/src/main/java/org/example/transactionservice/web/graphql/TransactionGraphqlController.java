package org.example.transactionservice.web.graphql;

import org.example.transactionservice.service.InvalidTransactionException;
import org.example.transactionservice.service.TransactionService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
public class TransactionGraphqlController {

    private final TransactionService transactionService;

    public TransactionGraphqlController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @QueryMapping
    public TransactionGraphqlView transaction(@Argument String id) {
        try {
            return TransactionGraphqlView.from(transactionService.findById(UUID.fromString(id)));
        } catch (InvalidTransactionException | IllegalArgumentException exception) {
            return null;
        }
    }

    @QueryMapping
    public List<TransactionGraphqlView> transactions(@Argument String accountId) {
        UUID filter = accountId == null || accountId.isBlank() ? null : UUID.fromString(accountId);
        return transactionService.findAll(filter).stream().map(TransactionGraphqlView::from).toList();
    }

    @MutationMapping
    public TransactionGraphqlView createTransaction(@Argument String accountId,
                                                     @Argument String amount,
                                                     @Argument String currency,
                                                     @Argument String merchantCategory,
                                                     @Argument String country) {
        return TransactionGraphqlView.from(transactionService.create(
                UUID.fromString(accountId),
                new BigDecimal(amount),
                currency,
                merchantCategory,
                country
        ));
    }

    @MutationMapping
    public List<TransactionGraphqlView> transfer(@Argument String sourceAccountId,
                                                  @Argument String destinationAccountId,
                                                  @Argument String amount,
                                                  @Argument String currency,
                                                  @Argument String country) {
        return transactionService.transfer(
                UUID.fromString(sourceAccountId),
                UUID.fromString(destinationAccountId),
                new BigDecimal(amount),
                currency,
                country
        ).stream().map(TransactionGraphqlView::from).toList();
    }
}
