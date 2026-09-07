package br.com.longobucco.personal_finance_app.core.domain;

import br.com.longobucco.personal_finance_app.core.exception.InvalidCategoryException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Category {

    private final UUID id;
    private String name;
    private final Type type;
    private final UUID ownerId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
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

        public BigDecimal reverse(BigDecimal balance, BigDecimal amount) {
            return apply(balance, amount.negate());
        }
    }
    private Category(UUID id, String name, Type type, UUID ownerId, LocalDateTime createdAt, LocalDateTime updatedAt){
        validate(name, type, ownerId);
        this.id = id;
        this.name = name;
        this.type = type;
        this.ownerId = ownerId;
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

    public UUID getOwnerId() {
        return ownerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isOwnedBy(UUID userId) {
        return ownerId.equals(userId);
    }

    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw InvalidCategoryException.blankName();
        }
        if (newName.equals(name)) {
            return;
        }
        this.name = newName;
        this.updatedAt = LocalDateTime.now();
    }

    public static Category createCategory(String name, Type type, UUID ownerId) {
        UUID id = UUID.randomUUID();
        LocalDateTime createAt = LocalDateTime.now();
        return new Category(id, name, type, ownerId, createAt, createAt);
    }

    public static Category recover(UUID id, String name, Type type, UUID ownerId, LocalDateTime createdAt,
                                   LocalDateTime updatedAt) {
        return new Category(id, name, type, ownerId, createdAt, updatedAt);
    }

    public static List<Category> defaultsFor(UUID ownerId) {
        return List.of(
                createCategory("Salário", Type.INCOME, ownerId),
                createCategory("Outras Receitas", Type.INCOME, ownerId),
                createCategory("Alimentação", Type.EXPENSE, ownerId),
                createCategory("Moradia", Type.EXPENSE, ownerId),
                createCategory("Transporte", Type.EXPENSE, ownerId),
                createCategory("Saúde", Type.EXPENSE, ownerId),
                createCategory("Educação", Type.EXPENSE, ownerId),
                createCategory("Lazer", Type.EXPENSE, ownerId),
                createCategory("Outras Despesas", Type.EXPENSE, ownerId));
    }

    private static void validate(String name, Type type, UUID ownerId) {
        if (name == null || name.isBlank()) {
            throw InvalidCategoryException.blankName();
        }
        if (type == null) {
            throw InvalidCategoryException.nullType();
        }
        if (ownerId == null) {
            throw InvalidCategoryException.nullOwnerId();
        }
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
