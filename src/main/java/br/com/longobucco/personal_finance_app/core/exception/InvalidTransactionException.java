package br.com.longobucco.personal_finance_app.core.exception;

import java.math.BigDecimal;

public class InvalidTransactionException extends DomainException {

    private InvalidTransactionException(String message) {
        super(message);
    }

    public static InvalidTransactionException blankDescription() {
        return new InvalidTransactionException("Transaction description must not be null or blank");
    }

    public static InvalidTransactionException nullCategory() {
        return new InvalidTransactionException("Transaction category must not be null");
    }

    public static InvalidTransactionException nonPositiveAmount(BigDecimal amount) {
        return new InvalidTransactionException("Transaction amount must be greater than zero: " + amount);
    }

    public static InvalidTransactionException nullUser() {
        return new InvalidTransactionException("Transaction user must not be null");
    }

    public static InvalidTransactionException nullPaymentMethod() {
        return new InvalidTransactionException("Transaction payment method must not be null");
    }
}
