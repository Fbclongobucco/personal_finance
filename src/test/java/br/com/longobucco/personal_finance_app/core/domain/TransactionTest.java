package br.com.longobucco.personal_finance_app.core.domain;

import br.com.longobucco.personal_finance_app.core.exception.InvalidTransactionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionTest {

    private static final LocalDate TODAY = LocalDate.now();

    private User validUser() {
        return User.createUser("John Doe", "john.doe@example.com", "11987654321", "secret123",
                new BigDecimal("100.00"));
    }

    private Category validCategory() {
        return Category.createCategory("Salary", Category.Type.INCOME);
    }

    @Test
    void createsTransactionWithValidDataAndRegistersItOnUser() {
        User user = validUser();
        Category category = validCategory();

        Transaction transaction = Transaction.create("Salary", category, new BigDecimal("50.00"), user,
                Transaction.PaymentMethod.PIX);

        assertNotNull(transaction.getId());
        assertTrue(user.getTransactions().contains(transaction));
    }

    @Test
    void throwsWhenDescriptionIsBlank() {
        User user = validUser();
        Category category = validCategory();

        assertThrows(InvalidTransactionException.class,
                () -> Transaction.create(" ", category, new BigDecimal("50.00"), user, Transaction.PaymentMethod.PIX));
    }

    @Test
    void throwsWhenCategoryIsNull() {
        User user = validUser();

        assertThrows(InvalidTransactionException.class,
                () -> Transaction.create("Salary", null, new BigDecimal("50.00"), user, Transaction.PaymentMethod.PIX));
    }

    @Test
    void throwsWhenAmountIsNull() {
        User user = validUser();
        Category category = validCategory();

        assertThrows(InvalidTransactionException.class,
                () -> Transaction.create("Salary", category, null, user, Transaction.PaymentMethod.PIX));
    }

    @Test
    void throwsWhenAmountIsZero() {
        User user = validUser();
        Category category = validCategory();

        assertThrows(InvalidTransactionException.class,
                () -> Transaction.create("Salary", category, BigDecimal.ZERO, user, Transaction.PaymentMethod.PIX));
    }

    @Test
    void throwsWhenAmountIsNegative() {
        User user = validUser();
        Category category = validCategory();

        assertThrows(InvalidTransactionException.class,
                () -> Transaction.create("Salary", category, new BigDecimal("-10.00"), user, Transaction.PaymentMethod.PIX));
    }

    @Test
    void throwsWhenUserIsNull() {
        Category category = validCategory();

        assertThrows(InvalidTransactionException.class,
                () -> Transaction.create("Salary", category, new BigDecimal("50.00"), null, Transaction.PaymentMethod.PIX));
    }

    @Test
    void throwsWhenPaymentMethodIsNull() {
        User user = validUser();
        Category category = validCategory();

        assertThrows(InvalidTransactionException.class,
                () -> Transaction.create("Salary", category, new BigDecimal("50.00"), user, null));
    }
}
