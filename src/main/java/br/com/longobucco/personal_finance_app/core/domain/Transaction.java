package br.com.longobucco.personal_finance_app.core.domain;

import br.com.longobucco.personal_finance_app.core.exception.InvalidTransactionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Transaction {

    private final UUID id;
    private final String description;
    private final Category category;
    private final BigDecimal amount;
    private final User user;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final PaymentMethod paymentMethod;
    public enum PaymentMethod {
        CASH, CREDIT_CARD, DEBIT_CARD, INVOICE, TICKET, PIX;
    }

    private Transaction(UUID id, String description, Category category, BigDecimal amount,
                        User user, LocalDateTime createdAt, LocalDateTime updatedAt, PaymentMethod paymentMethod){
        validate(description, category, amount, user, paymentMethod);
        this.id = id;
        this.description = description;
        this.category = category;
        this.amount = amount;
        this.user = user;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.paymentMethod = paymentMethod;
    }

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public static Transaction create(String description, Category category, BigDecimal amount,
                                     User user, PaymentMethod paymentMethod){
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();
        Transaction transaction = new Transaction(id, description, category, amount, user, createdAt, createdAt, paymentMethod);
        user.addTransaction(transaction);
        return transaction;
    }

    public static Transaction recover(UUID id, String description, Category category, BigDecimal amount,
                                      User user, LocalDateTime createdAt, LocalDateTime updatedAt, PaymentMethod paymentMethod){
        Transaction transaction = new Transaction(id, description, category, amount, user, createdAt, updatedAt, paymentMethod);
        user.addTransaction(transaction);
        return transaction;
    }

    private static void validate(String description, Category category, BigDecimal amount, User user, PaymentMethod paymentMethod) {
        if (description == null || description.isBlank()) {
            throw InvalidTransactionException.blankDescription();
        }
        if (category == null) {
            throw InvalidTransactionException.nullCategory();
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw InvalidTransactionException.nonPositiveAmount(amount);
        }
        if (user == null) {
            throw InvalidTransactionException.nullUser();
        }
        if (paymentMethod == null) {
            throw InvalidTransactionException.nullPaymentMethod();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
