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
    private final UUID userId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final PaymentMethod paymentMethod;
    private boolean paid;
    public enum PaymentMethod {
        CASH, CREDIT_CARD, DEBIT_CARD, INVOICE, TICKET, PIX;
    }

    private Transaction(UUID id, String description, Category category, BigDecimal amount,
                        UUID userId, LocalDateTime createdAt, LocalDateTime updatedAt, PaymentMethod paymentMethod,
                        boolean paid){
        validate(description, category, amount, userId, paymentMethod);
        this.id = id;
        this.description = description;
        this.category = category;
        this.amount = amount;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.paymentMethod = paymentMethod;

        this.paid = category.getType() == Category.Type.EXPENSE ? paid : true;
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

    public UUID getUserId() {
        return userId;
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

    public boolean isPaid() {
        return paid;
    }

    public void settle() {
        if (category.getType() != Category.Type.EXPENSE) {
            throw InvalidTransactionException.notExpense();
        }
        if (paid) {
            return;
        }
        this.paid = true;
        this.updatedAt = LocalDateTime.now();
    }

    public static Transaction create(String description, Category category, BigDecimal amount,
                                     UUID userId, PaymentMethod paymentMethod){
        return create(description, category, amount, userId, paymentMethod, defaultPaid(category), null);
    }

    public static Transaction create(String description, Category category, BigDecimal amount,
                                     UUID userId, PaymentMethod paymentMethod, boolean paid){
        return create(description, category, amount, userId, paymentMethod, paid, null);
    }

    public static Transaction create(String description, Category category, BigDecimal amount,
                                     UUID userId, PaymentMethod paymentMethod, LocalDateTime date){
        return create(description, category, amount, userId, paymentMethod, defaultPaid(category), date);
    }

    public static Transaction create(String description, Category category, BigDecimal amount,
                                     UUID userId, PaymentMethod paymentMethod, boolean paid,
                                     LocalDateTime date){
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = date != null ? date : LocalDateTime.now();
        return new Transaction(id, description, category, amount, userId, createdAt, createdAt, paymentMethod, paid);
    }

    public static Transaction recover(UUID id, String description, Category category, BigDecimal amount,
                                      UUID userId, LocalDateTime createdAt, LocalDateTime updatedAt,
                                      PaymentMethod paymentMethod){
        return recover(id, description, category, amount, userId, createdAt, updatedAt, paymentMethod,
                defaultPaid(category));
    }

    public static Transaction recover(UUID id, String description, Category category, BigDecimal amount,
                                      UUID userId, LocalDateTime createdAt, LocalDateTime updatedAt,
                                      PaymentMethod paymentMethod, boolean paid){
        return new Transaction(id, description, category, amount, userId, createdAt, updatedAt, paymentMethod, paid);
    }

    private static boolean defaultPaid(Category category) {
        return category == null || category.getType() != Category.Type.EXPENSE;
    }

    private static void validate(String description, Category category, BigDecimal amount, UUID userId,
                                 PaymentMethod paymentMethod) {
        if (description == null || description.isBlank()) {
            throw InvalidTransactionException.blankDescription();
        }
        if (category == null) {
            throw InvalidTransactionException.nullCategory();
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw InvalidTransactionException.nonPositiveAmount(amount);
        }
        if (userId == null) {
            throw InvalidTransactionException.nullUserId();
        }
        if (paymentMethod == null) {
            throw InvalidTransactionException.nullPaymentMethod();
        }

        if (!category.isOwnedBy(userId)) {
            throw InvalidTransactionException.categoryNotOwnedByUser(category.getId(), userId);
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
