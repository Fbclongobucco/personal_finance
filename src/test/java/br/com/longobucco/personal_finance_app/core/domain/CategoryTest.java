package br.com.longobucco.personal_finance_app.core.domain;

import br.com.longobucco.personal_finance_app.core.exception.InvalidCategoryException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryTest {

    @Test
    void createsCategoryWithValidData() {
        UUID ownerId = UUID.randomUUID();
        Category category = Category.createCategory("Salary", Category.Type.INCOME, ownerId);

        assertNotNull(category.getId());
        assertEquals("Salary", category.getName());
        assertEquals(Category.Type.INCOME, category.getType());
        assertEquals(ownerId, category.getOwnerId());
        assertNotNull(category.getCreatedAt());
        assertNotNull(category.getUpdatedAt());
    }

    @Test
    void throwsWhenNameIsNull() {
        assertThrows(InvalidCategoryException.class,
                () -> Category.createCategory(null, Category.Type.INCOME, UUID.randomUUID()));
    }

    @Test
    void throwsWhenNameIsBlank() {
        assertThrows(InvalidCategoryException.class,
                () -> Category.createCategory("   ", Category.Type.INCOME, UUID.randomUUID()));
    }

    @Test
    void throwsWhenTypeIsNull() {
        assertThrows(InvalidCategoryException.class,
                () -> Category.createCategory("Salary", null, UUID.randomUUID()));
    }

    @Test
    void throwsWhenOwnerIdIsNull() {
        assertThrows(InvalidCategoryException.class,
                () -> Category.createCategory("Salary", Category.Type.INCOME, null));
    }

    @Test
    void isOwnedByOnlyRecognisesTheOwner() {
        UUID ownerId = UUID.randomUUID();
        Category category = Category.createCategory("Salary", Category.Type.INCOME, ownerId);

        assertTrue(category.isOwnedBy(ownerId));
        assertFalse(category.isOwnedBy(UUID.randomUUID()));
    }

    @Test
    void renameChangesTheNameAndTouchesUpdatedAt() {
        Category category = Category.recover(UUID.randomUUID(), "Salary", Category.Type.INCOME, UUID.randomUUID(),
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 1, 0, 0));

        category.rename("Salário");

        assertEquals("Salário", category.getName());
        assertTrue(category.getUpdatedAt().isAfter(category.getCreatedAt()));
    }

    @Test
    void renameToTheSameNameLeavesUpdatedAtAlone() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        Category category = Category.recover(UUID.randomUUID(), "Salary", Category.Type.INCOME, UUID.randomUUID(),
                createdAt, createdAt);

        category.rename("Salary");

        assertEquals(createdAt, category.getUpdatedAt());
    }

    @Test
    void renameThrowsWhenNewNameIsBlank() {
        Category category = Category.createCategory("Salary", Category.Type.INCOME, UUID.randomUUID());

        assertThrows(InvalidCategoryException.class, () -> category.rename("  "));
    }

    @Test
    void defaultsForGivesTheOwnerAStarterSetOfBothTypes() {
        UUID ownerId = UUID.randomUUID();

        List<Category> defaults = Category.defaultsFor(ownerId);

        assertFalse(defaults.isEmpty());
        assertTrue(defaults.stream().allMatch(category -> category.isOwnedBy(ownerId)));
        assertTrue(defaults.stream().anyMatch(category -> category.getType() == Category.Type.INCOME));
        assertTrue(defaults.stream().anyMatch(category -> category.getType() == Category.Type.EXPENSE));
        assertEquals(defaults.size(), defaults.stream().map(Category::getName).distinct().count());
    }

    @Test
    void incomeTypeAddsAmountToBalance() {
        BigDecimal result = Category.Type.INCOME.apply(new BigDecimal("100"), new BigDecimal("50"));

        assertEquals(new BigDecimal("150"), result);
    }

    @Test
    void expenseTypeSubtractsAmountFromBalance() {
        BigDecimal result = Category.Type.EXPENSE.apply(new BigDecimal("100"), new BigDecimal("50"));

        assertEquals(new BigDecimal("50"), result);
    }

    @Test
    void reverseUndoesIncomeApply() {
        BigDecimal result = Category.Type.INCOME.reverse(new BigDecimal("150"), new BigDecimal("50"));

        assertEquals(new BigDecimal("100"), result);
    }

    @Test
    void reverseUndoesExpenseApply() {
        BigDecimal result = Category.Type.EXPENSE.reverse(new BigDecimal("50"), new BigDecimal("50"));

        assertEquals(new BigDecimal("100"), result);
    }
}
