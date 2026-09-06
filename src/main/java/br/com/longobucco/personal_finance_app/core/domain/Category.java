package br.com.longobucco.personal_finance_app.core.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Category {

    private UUID id;
    private String name;
    private Type type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public enum Type {
        INCOME, EXPENSE;
    }
    private Category(UUID id, String name, Type type, LocalDateTime createdAt, LocalDateTime updatedAt){
        this.id = id;
        this.name = name;
        this.type = type;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static Category createCategory(String name, Type type) {
        UUID id = UUID.randomUUID();
        LocalDateTime createAt = LocalDateTime.now();
        return new Category(id, name, type, createAt, createAt);
    }

    public static Category recover(UUID id, String name, Type type, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Category(id, name, type, createdAt, updatedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
