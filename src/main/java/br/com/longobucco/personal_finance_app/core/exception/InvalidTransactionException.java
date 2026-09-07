package br.com.longobucco.personal_finance_app.core.exception;

import java.math.BigDecimal;
import java.util.UUID;

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

    public static InvalidTransactionException nullUserId() {
        return new InvalidTransactionException("Transaction user must not be null");
    }

    public static InvalidTransactionException categoryNotOwnedByUser(UUID categoryId, UUID userId) {
        return new InvalidTransactionException(
                "Category '%s' does not belong to user '%s'".formatted(categoryId, userId));
    }

    public static InvalidTransactionException nullPaymentMethod() {
        return new InvalidTransactionException("Transaction payment method must not be null");
    }

    public static InvalidTransactionException notExpense() {
        return new InvalidTransactionException("Only EXPENSE transactions can be settled");
    }
}
