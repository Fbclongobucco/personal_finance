package br.com.longobucco.personal_finance_app.core.domain;

import br.com.longobucco.personal_finance_app.core.exception.InvalidUserException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    private Transaction incomeOf(User user, BigDecimal amount) {
        Category income = Category.createCategory("Salary", Category.Type.INCOME, user.getId());
        return Transaction.create("Salary", income, amount, user.getId(), Transaction.PaymentMethod.PIX);
    }

    private Transaction expenseOf(User user, BigDecimal amount) {
        Category expense = Category.createCategory("Rent", Category.Type.EXPENSE, user.getId());
        return Transaction.create("Rent", expense, amount, user.getId(), Transaction.PaymentMethod.CASH);
    }

    @Test
    void createsUserWithValidData() {
        User user = validUser();

        assertNotNull(user.getId());
        assertEquals("John Doe", user.getName());
        assertEquals("john.doe@example.com", user.getEmail());
        assertEquals("11987654321", user.getPhone());
        assertEquals(User.Role.USER, user.getRole());
        assertFalse(user.isAdmin());
        assertEquals(new BigDecimal("100.00"), user.getBalance());
    }

    @Test
    void createsAdminWithAdminRole() {
        User admin = User.createAdmin("Jane Doe", "jane.doe@example.com", "11987654321", "secret123",
                BigDecimal.ZERO, TODAY, TODAY);

        assertEquals(User.Role.ADMIN, admin.getRole());
        assertTrue(admin.isAdmin());
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
    void applyingIncomeTransactionIncreasesBalance() {
        User user = validUser();

        user.applyTransaction(incomeOf(user, new BigDecimal("50.00")));

        assertEquals(new BigDecimal("150.00"), user.getBalance());
    }

    @Test
    void applyingExpenseTransactionDecreasesBalance() {
        User user = validUser();

        user.applyTransaction(expenseOf(user, new BigDecimal("30.00")));

        assertEquals(new BigDecimal("70.00"), user.getBalance());
    }

    @Test
    void reversingIncomeTransactionRestoresBalance() {
        User user = validUser();
        Transaction transaction = incomeOf(user, new BigDecimal("50.00"));
        user.applyTransaction(transaction);

        user.reverseTransaction(transaction);

        assertEquals(new BigDecimal("100.00"), user.getBalance());
    }

    @Test
    void reversingExpenseTransactionRestoresBalance() {
        User user = validUser();
        Transaction transaction = expenseOf(user, new BigDecimal("30.00"));
        user.applyTransaction(transaction);

        user.reverseTransaction(transaction);

        assertEquals(new BigDecimal("100.00"), user.getBalance());
    }

    @Test
    void throwsWhenApplyingATransactionThatBelongsToAnotherUser() {
        User user = validUser();
        User other = User.createUser("Jane Doe", "jane.doe@example.com", "11987654321", "secret123",
                BigDecimal.ZERO);
        Transaction othersTransaction = incomeOf(other, new BigDecimal("50.00"));

        assertThrows(InvalidUserException.class, () -> user.applyTransaction(othersTransaction));
    }

    @Test
    void throwsWhenReversingATransactionThatBelongsToAnotherUser() {
        User user = validUser();
        User other = User.createUser("Jane Doe", "jane.doe@example.com", "11987654321", "secret123",
                BigDecimal.ZERO);
        Transaction othersTransaction = incomeOf(other, new BigDecimal("50.00"));

        assertThrows(InvalidUserException.class, () -> user.reverseTransaction(othersTransaction));
    }

    @Test
    void settledBalanceAddsBackTheStillPendingExpenses() {
        User user = validUser();
        user.applyTransaction(expenseOf(user, new BigDecimal("30.00")));

        assertEquals(new BigDecimal("70.00"), user.getBalance());
        assertEquals(new BigDecimal("100.00"), user.settledBalance(new BigDecimal("30.00")));
    }

    @Test
    void settledBalanceEqualsBalanceWhenNothingIsPending() {
        User user = validUser();
        user.applyTransaction(incomeOf(user, new BigDecimal("50.00")));

        assertEquals(user.getBalance(), user.settledBalance(BigDecimal.ZERO));
    }

    @Test
    void settledBalanceThrowsWhenPendingTotalIsNull() {
        User user = validUser();

        assertThrows(InvalidUserException.class, () -> user.settledBalance(null));
    }

    @Test
    void settledBalanceThrowsWhenPendingTotalIsNegative() {
        User user = validUser();

        assertThrows(InvalidUserException.class, () -> user.settledBalance(new BigDecimal("-1.00")));
    }

    @Test
    void recoverKeepsTheProvidedIdentityAndRole() {
        UUID id = UUID.randomUUID();

        User user = User.recover(id, "John Doe", "john.doe@example.com", "11987654321", "hashed",
                User.Role.ADMIN, new BigDecimal("10.00"), TODAY, TODAY);

        assertEquals(id, user.getId());
        assertTrue(user.isAdmin());
    }
}
