package br.com.longobucco.personal_finance_app.core.exception;

import java.math.BigDecimal;

public class InvalidUserException extends DomainException {

    private InvalidUserException(String message) {
        super(message);
    }

    public static InvalidUserException blankName() {
        return new InvalidUserException("User name must not be null or blank");
    }

    public static InvalidUserException invalidEmail(String email) {
        return new InvalidUserException("User email '%s' is not a valid email address".formatted(email));
    }

    public static InvalidUserException blankPassword() {
        return new InvalidUserException("User password must not be null or blank");
    }

    public static InvalidUserException invalidPhone(String phone) {
        return new InvalidUserException("User phone '%s' is not a valid phone number".formatted(phone));
    }

    public static InvalidUserException nullInitialBalance() {
        return new InvalidUserException("User initial balance must not be null");
    }

    public static InvalidUserException invalidPendingExpenseTotal(BigDecimal total) {
        return new InvalidUserException("Pending expense total must not be null or negative: " + total);
    }

    public static InvalidUserException foreignTransaction() {
        return new InvalidUserException("A transaction belonging to another user cannot affect this user's balance");
    }
}
