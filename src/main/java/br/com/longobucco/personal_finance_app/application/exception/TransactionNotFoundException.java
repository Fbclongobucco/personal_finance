package br.com.longobucco.personal_finance_app.application.exception;

import java.util.UUID;

public class TransactionNotFoundException extends ApplicationException {

    private TransactionNotFoundException(String message) {
        super(message);
    }

    public static TransactionNotFoundException withId(UUID id) {
        return new TransactionNotFoundException("Transaction with id '%s' was not found".formatted(id));
    }
}
