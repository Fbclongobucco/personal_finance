package br.com.longobucco.personal_finance_app.core.domain;

import br.com.longobucco.personal_finance_app.core.exception.InvalidPeriodException;
import br.com.longobucco.personal_finance_app.core.exception.InvalidUserException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    private static final LocalDate TODAY = LocalDate.now();

    private User validUser() {
        return User.createUser("John Doe", "john.doe@example.com", "11987654321", "secret123",
                new BigDecimal("100.00"));
    }

    @Test
    void createsUserWithValidData() {
        User user = validUser();

        assertNotNull(user.getId());
        assertEquals("John Doe", user.getName());
        assertEquals("john.doe@example.com", user.getEmail());
        assertEquals("11987654321", user.getPhone());
        assertEquals(User.Role.USER, user.getRole());
        assertEquals(new BigDecimal("100.00"), user.getBalance());
        assertTrue(user.getTransactions().isEmpty());
    }

    @Test
    void createsAdminWithAdminRole() {
        User admin = User.createAdmin("Jane Doe", "jane.doe@example.com", "11987654321", "secret123",
                BigDecimal.ZERO, TODAY, TODAY);

        assertEquals(User.Role.ADMIN, admin.getRole());
    }

    @Test
    void throwsWhenNameIsBlank() {
        assertThrows(InvalidUserException.class,
                () -> User.createUser(" ", "john.doe@example.com", "11987654321", "secret123",
                        BigDecimal.ZERO));
    }

    @Test
    void throwsWhenEmailIsNull() {
        assertThrows(InvalidUserException.class,
                () -> User.createUser("John Doe", null, "11987654321", "secret123",
                        BigDecimal.ZERO));
    }

    @Test
    void throwsWhenEmailFormatIsInvalid() {
        assertThrows(InvalidUserException.class,
                () -> User.createUser("John Doe", "not-an-email", "11987654321", "secret123",
                        BigDecimal.ZERO));
    }

    @Test
    void throwsWhenPhoneIsNull() {
        assertThrows(InvalidUserException.class,
                () -> User.createUser("John Doe", "john.doe@example.com", null, "secret123",
                        BigDecimal.ZERO));
    }

    @Test
    void throwsWhenPhoneHasTooFewDigits() {
        assertThrows(InvalidUserException.class,
                () -> User.createUser("John Doe", "john.doe@example.com", "1234", "secret123",
                        BigDecimal.ZERO));
    }

    @Test
    void acceptsPhoneWithFormattingCharacters() {
        User user = User.createUser("John Doe", "john.doe@example.com", "(11) 98765-4321", "secret123",
                BigDecimal.ZERO);

        assertEquals("(11) 98765-4321", user.getPhone());
    }

    @Test
    void throwsWhenPasswordIsBlank() {
        assertThrows(InvalidUserException.class,
                () -> User.createUser("John Doe", "john.doe@example.com", "11987654321", " ",
                        BigDecimal.ZERO));
    }

    @Test
    void throwsWhenInitialBalanceIsNull() {
        assertThrows(InvalidUserException.class,
                () -> User.createUser("John Doe", "john.doe@example.com", "11987654321", "secret123",
                        null));
    }

    @Test
    void getTransactionsIsUnmodifiable() {
        User user = validUser();

        assertThrows(UnsupportedOperationException.class,
                () -> user.getTransactions().add(null));
    }

    @Test
    void addingIncomeTransactionIncreasesBalance() {
        User user = validUser();
        Category income = Category.createCategory("Salary", Category.Type.INCOME);

        Transaction.create("Salary", income, new BigDecimal("50.00"), user, Transaction.PaymentMethod.PIX);

        assertEquals(new BigDecimal("150.00"), user.getBalance());
    }

    @Test
    void addingExpenseTransactionDecreasesBalance() {
        User user = validUser();
        Category expense = Category.createCategory("Rent", Category.Type.EXPENSE);

        Transaction.create("Rent", expense, new BigDecimal("30.00"), user, Transaction.PaymentMethod.CASH);

        assertEquals(new BigDecimal("70.00"), user.getBalance());
    }

    @Test
    void removingIncomeTransactionReversesBalance() {
        User user = validUser();
        Category income = Category.createCategory("Salary", Category.Type.INCOME);
        Transaction transaction = Transaction.create("Salary", income, new BigDecimal("50.00"), user,
                Transaction.PaymentMethod.PIX);

        user.removeTransaction(transaction);

        assertEquals(new BigDecimal("100.00"), user.getBalance());
        assertFalse(user.getTransactions().contains(transaction));
    }

    @Test
    void removingExpenseTransactionReversesBalance() {
        User user = validUser();
        Category expense = Category.createCategory("Rent", Category.Type.EXPENSE);
        Transaction transaction = Transaction.create("Rent", expense, new BigDecimal("30.00"), user,
                Transaction.PaymentMethod.CASH);

        user.removeTransaction(transaction);

        assertEquals(new BigDecimal("100.00"), user.getBalance());
        assertFalse(user.getTransactions().contains(transaction));
    }

    @Test
    void attachingAReconstitutedTransactionDoesNotReapplyItsEffectOnBalance() {
        User user = validUser();
        Category income = Category.createCategory("Salary", Category.Type.INCOME);

        Transaction.recover(UUID.randomUUID(), "Salary", income, new BigDecimal("50.00"), user,
                LocalDateTime.now(), LocalDateTime.now(), Transaction.PaymentMethod.PIX);

        assertEquals(new BigDecimal("100.00"), user.getBalance());
    }

    private Transaction transactionAt(User user, Category category, BigDecimal amount, LocalDateTime createdAt) {
        return Transaction.recover(UUID.randomUUID(), "Transaction", category, amount, user,
                createdAt, createdAt, Transaction.PaymentMethod.CASH);
    }

    @Test
    void getTransactionsBetweenReturnsOnlyTransactionsWithinRange() {
        User user = validUser();
        Category expense = Category.createCategory("Rent", Category.Type.EXPENSE);
        Transaction inRange = transactionAt(user, expense, new BigDecimal("30.00"),
                LocalDateTime.of(2026, 1, 15, 10, 0));
        Transaction outOfRange = transactionAt(user, expense, new BigDecimal("40.00"),
                LocalDateTime.of(2026, 3, 1, 10, 0));

        List<Transaction> result = user.getTransactionsBetween(
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 31, 23, 59));

        assertTrue(result.contains(inRange));
        assertFalse(result.contains(outOfRange));
    }

    @Test
    void getExpensesBetweenReturnsOnlyExpenseTransactions() {
        User user = validUser();
        Category income = Category.createCategory("Salary", Category.Type.INCOME);
        Category expense = Category.createCategory("Rent", Category.Type.EXPENSE);
        LocalDateTime date = LocalDateTime.of(2026, 1, 15, 10, 0);
        Transaction rent = transactionAt(user, expense, new BigDecimal("30.00"), date);
        Transaction salary = transactionAt(user, income, new BigDecimal("1000.00"), date);

        List<Transaction> result = user.getExpensesBetween(
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 31, 23, 59));

        assertTrue(result.contains(rent));
        assertFalse(result.contains(salary));
    }

    @Test
    void getIncomesBetweenReturnsOnlyIncomeTransactions() {
        User user = validUser();
        Category income = Category.createCategory("Salary", Category.Type.INCOME);
        Category expense = Category.createCategory("Rent", Category.Type.EXPENSE);
        LocalDateTime date = LocalDateTime.of(2026, 1, 15, 10, 0);
        Transaction rent = transactionAt(user, expense, new BigDecimal("30.00"), date);
        Transaction salary = transactionAt(user, income, new BigDecimal("1000.00"), date);

        List<Transaction> result = user.getIncomesBetween(
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 31, 23, 59));

        assertTrue(result.contains(salary));
        assertFalse(result.contains(rent));
    }

    @Test
    void throwsInvalidPeriodExceptionWhenStartIsNull() {
        User user = validUser();

        assertThrows(InvalidPeriodException.class,
                () -> user.getTransactionsBetween(null, LocalDateTime.now()));
    }

    @Test
    void throwsInvalidPeriodExceptionWhenEndIsNull() {
        User user = validUser();

        assertThrows(InvalidPeriodException.class,
                () -> user.getTransactionsBetween(LocalDateTime.now(), null));
    }

    @Test
    void throwsInvalidPeriodExceptionWhenStartIsAfterEnd() {
        User user = validUser();
        LocalDateTime start = LocalDateTime.of(2026, 2, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 1, 0, 0);

        assertThrows(InvalidPeriodException.class,
                () -> user.getTransactionsBetween(start, end));
    }
}
