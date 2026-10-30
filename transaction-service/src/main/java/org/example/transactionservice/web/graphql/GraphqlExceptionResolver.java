package org.example.transactionservice.web.graphql;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.example.transactionservice.service.AccountNotFoundException;
import org.example.transactionservice.service.AccountServiceUnavailableException;
import org.example.transactionservice.service.InvalidTransactionException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

@Component
public class GraphqlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable exception, DataFetchingEnvironment environment) {
        if (exception instanceof AccountNotFoundException) {
            return error(environment, exception, ErrorType.NOT_FOUND);
        }
        if (exception instanceof InvalidTransactionException) {
            return error(environment, exception, ErrorType.BAD_REQUEST);
        }
        if (exception instanceof AccountServiceUnavailableException) {
            return error(environment, exception, ErrorType.INTERNAL_ERROR);
        }
        return null;
    }

    private static GraphQLError error(DataFetchingEnvironment environment, Throwable exception, ErrorType errorType) {
        return GraphqlErrorBuilder.newError(environment)
                .message(exception.getMessage())
                .errorType(errorType)
                .build();
    }
}
