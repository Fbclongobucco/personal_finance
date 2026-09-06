package br.com.longobucco.personal_finance_app.core.domain;

import br.com.longobucco.personal_finance_app.core.exception.InvalidCategoryException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Category {

    private final UUID id;
    private final String name;
    private final Type type;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    public enum Type {
        INCOME {
            @Override
            public BigDecimal apply(BigDecimal balance, BigDecimal amount) {
                return balance.add(amount);
            }
        },
        EXPENSE {
            @Override
            public BigDecimal apply(BigDecimal balance, BigDecimal amount) {
                return balance.subtract(amount);
            }
        };

        public abstract BigDecimal apply(BigDecimal balance, BigDecimal amount);
    }
    private Category(UUID id, String name, Type type, LocalDateTime createdAt, LocalDateTime updatedAt){
        if (name == null || name.isBlank()) {
            throw InvalidCategoryException.blankName();
        }
        if (type == null) {
            throw InvalidCategoryException.nullType();
        }
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
