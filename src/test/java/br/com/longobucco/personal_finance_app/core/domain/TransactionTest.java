package br.com.longobucco.personal_finance_app.core.domain;

import br.com.longobucco.personal_finance_app.core.exception.InvalidTransactionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionTest {

    private final UUID userId = UUID.randomUUID();

    private Category incomeCategory() {
        return Category.createCategory("Salary", Category.Type.INCOME, userId);
    }

    private Category expenseCategory() {
        return Category.createCategory("Rent", Category.Type.EXPENSE, userId);
    }

    @Test
    void createsTransactionWithValidData() {
        Category category = incomeCategory();

        Transaction transaction = Transaction.create("Salary", category, new BigDecimal("50.00"), userId,
                Transaction.PaymentMethod.PIX);

        assertNotNull(transaction.getId());
        assertEquals("Salary", transaction.getDescription());
        assertEquals(category, transaction.getCategory());
        assertEquals(userId, transaction.getUserId());
        assertEquals(Transaction.PaymentMethod.PIX, transaction.getPaymentMethod());
    }

    @Test
    void throwsWhenDescriptionIsBlank() {
        Category category = incomeCategory();

        assertThrows(InvalidTransactionException.class,
                () -> Transaction.create(" ", category, new BigDecimal("50.00"), userId, Transaction.PaymentMethod.PIX));
    }

    @Test
    void throwsWhenCategoryIsNull() {
        assertThrows(InvalidTransactionException.class,
                () -> Transaction.create("Salary", null, new BigDecimal("50.00"), userId, Transaction.PaymentMethod.PIX));
    }

    @Test
    void throwsWhenAmountIsNull() {
        Category category = incomeCategory();

        assertThrows(InvalidTransactionException.class,
                () -> Transaction.create("Salary", category, null, userId, Transaction.PaymentMethod.PIX));
    }

    @Test
    void throwsWhenAmountIsZero() {
        Category category = incomeCategory();

        assertThrows(InvalidTransactionException.class,
                () -> Transaction.create("Salary", category, BigDecimal.ZERO, userId, Transaction.PaymentMethod.PIX));
    }

    @Test
    void throwsWhenAmountIsNegative() {
        Category category = incomeCategory();

        assertThrows(InvalidTransactionException.class,
                () -> Transaction.create("Salary", category, new BigDecimal("-10.00"), userId,
                        Transaction.PaymentMethod.PIX));
    }

    @Test
    void throwsWhenUserIdIsNull() {
        Category category = incomeCategory();

        assertThrows(InvalidTransactionException.class,
                () -> Transaction.create("Salary", category, new BigDecimal("50.00"), null,
                        Transaction.PaymentMethod.PIX));
    }

    @Test
    void throwsWhenPaymentMethodIsNull() {
        Category category = incomeCategory();

        assertThrows(InvalidTransactionException.class,
                () -> Transaction.create("Salary", category, new BigDecimal("50.00"), userId, null));
    }

    @Test
    void throwsWhenCategoryBelongsToAnotherUser() {
        Category othersCategory = Category.createCategory("Salary", Category.Type.INCOME, UUID.randomUUID());

        assertThrows(InvalidTransactionException.class,
                () -> Transaction.create("Salary", othersCategory, new BigDecimal("50.00"), userId,
                        Transaction.PaymentMethod.PIX));
    }

    @Test
    void recoverAlsoRejectsACategoryFromAnotherUser() {
        Category othersCategory = Category.createCategory("Salary", Category.Type.INCOME, UUID.randomUUID());
        LocalDateTime now = LocalDateTime.now();

        assertThrows(InvalidTransactionException.class,
                () -> Transaction.recover(UUID.randomUUID(), "Salary", othersCategory, new BigDecimal("50.00"),
                        userId, now, now, Transaction.PaymentMethod.PIX));
    }

    @Test
    void incomeTransactionIsAlwaysPaid() {
        Transaction transaction = Transaction.create("Salary", incomeCategory(), new BigDecimal("50.00"), userId,
                Transaction.PaymentMethod.PIX);

        assertTrue(transaction.isPaid());
    }

    @Test
    void incomeStaysPaidEvenWhenExplicitlyCreatedUnpaid() {
        Transaction transaction = Transaction.create("Salary", incomeCategory(), new BigDecimal("50.00"), userId,
                Transaction.PaymentMethod.PIX, false);

        assertTrue(transaction.isPaid());
    }

    @Test
    void expenseTransactionStartsUnpaid() {
        Transaction transaction = Transaction.create("Rent", expenseCategory(), new BigDecimal("30.00"), userId,
                Transaction.PaymentMethod.CASH);

        assertFalse(transaction.isPaid());
    }

    @Test
    void settleMarksAnExpenseAsPaidAndTouchesUpdatedAt() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        Transaction transaction = Transaction.recover(UUID.randomUUID(), "Rent", expenseCategory(),
                new BigDecimal("30.00"), userId, createdAt, createdAt, Transaction.PaymentMethod.CASH, false);

        transaction.settle();

        assertTrue(transaction.isPaid());
        assertTrue(transaction.getUpdatedAt().isAfter(createdAt));
    }

    @Test
    void settlingAnAlreadySettledExpenseLeavesUpdatedAtAlone() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        Transaction transaction = Transaction.recover(UUID.randomUUID(), "Rent", expenseCategory(),
                new BigDecimal("30.00"), userId, createdAt, createdAt, Transaction.PaymentMethod.CASH, true);

        transaction.settle();

        assertTrue(transaction.isPaid());
        assertEquals(createdAt, transaction.getUpdatedAt());
    }

    @Test
    void settleThrowsForAnIncomeTransaction() {
        Transaction transaction = Transaction.create("Salary", incomeCategory(), new BigDecimal("50.00"), userId,
                Transaction.PaymentMethod.PIX);

        assertThrows(InvalidTransactionException.class, transaction::settle);
    }
}
