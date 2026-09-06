package br.com.longobucco.personal_finance_app.core.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Transaction {

    private UUID id;
    private String description;
    private Category category;
    private BigDecimal amount;
    private User user;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private PaymentMethod paymentMethod;
    public enum PaymentMethod {
        CASH, CREDIT_CARD, DEBIT_CARD, INVOICE, TICKET, PIX;
    }

    private Transaction(UUID id, String description, Category category, BigDecimal amount,
                        User user, LocalDateTime createdAt, LocalDateTime updatedAt, PaymentMethod paymentMethod){
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
        return new Transaction(id, description, category, amount, user, createdAt, createdAt, paymentMethod);
    }

    public static Transaction recover(UUID id, String description, Category category, BigDecimal amount,
                                      User user, LocalDateTime createdAt, LocalDateTime updatedAt, PaymentMethod paymentMethod){
        return new Transaction(id, description, category, amount, user, createdAt, updatedAt, paymentMethod);
    }
}
