package br.com.longobucco.personal_finance_app.core.domain;

import br.com.longobucco.personal_finance_app.core.exception.InvalidCategoryException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CategoryTest {

    @Test
    void createsCategoryWithValidData() {
        Category category = Category.createCategory("Salary", Category.Type.INCOME);

        assertNotNull(category.getId());
        assertEquals("Salary", category.getName());
        assertEquals(Category.Type.INCOME, category.getType());
        assertNotNull(category.getCreatedAt());
        assertNotNull(category.getUpdatedAt());
    }

    @Test
    void throwsWhenNameIsNull() {
        assertThrows(InvalidCategoryException.class,
                () -> Category.createCategory(null, Category.Type.INCOME));
    }

    @Test
    void throwsWhenNameIsBlank() {
        assertThrows(InvalidCategoryException.class,
                () -> Category.createCategory("   ", Category.Type.INCOME));
    }

    @Test
    void throwsWhenTypeIsNull() {
        assertThrows(InvalidCategoryException.class,
                () -> Category.createCategory("Salary", null));
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
