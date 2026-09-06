package br.com.longobucco.personal_finance_app.core.domain;

import br.com.longobucco.personal_finance_app.core.exception.InvalidUserException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    private static final LocalDate TODAY = LocalDate.now();

    private User validUser() {
        return User.createUser("John Doe", "john.doe@example.com", "11987654321", "secret123",
                new BigDecimal("100.00"), TODAY, TODAY);
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
                        BigDecimal.ZERO, TODAY, TODAY));
    }

    @Test
    void throwsWhenEmailIsNull() {
        assertThrows(InvalidUserException.class,
                () -> User.createUser("John Doe", null, "11987654321", "secret123",
                        BigDecimal.ZERO, TODAY, TODAY));
    }

    @Test
    void throwsWhenEmailFormatIsInvalid() {
        assertThrows(InvalidUserException.class,
                () -> User.createUser("John Doe", "not-an-email", "11987654321", "secret123",
                        BigDecimal.ZERO, TODAY, TODAY));
    }

    @Test
    void throwsWhenPhoneIsNull() {
        assertThrows(InvalidUserException.class,
                () -> User.createUser("John Doe", "john.doe@example.com", null, "secret123",
                        BigDecimal.ZERO, TODAY, TODAY));
    }

    @Test
    void throwsWhenPhoneHasTooFewDigits() {
        assertThrows(InvalidUserException.class,
                () -> User.createUser("John Doe", "john.doe@example.com", "1234", "secret123",
                        BigDecimal.ZERO, TODAY, TODAY));
    }

    @Test
    void acceptsPhoneWithFormattingCharacters() {
        User user = User.createUser("John Doe", "john.doe@example.com", "(11) 98765-4321", "secret123",
                BigDecimal.ZERO, TODAY, TODAY);

        assertEquals("(11) 98765-4321", user.getPhone());
    }

    @Test
    void throwsWhenPasswordIsBlank() {
        assertThrows(InvalidUserException.class,
                () -> User.createUser("John Doe", "john.doe@example.com", "11987654321", " ",
                        BigDecimal.ZERO, TODAY, TODAY));
    }

    @Test
    void throwsWhenInitialBalanceIsNull() {
        assertThrows(InvalidUserException.class,
                () -> User.createUser("John Doe", "john.doe@example.com", "11987654321", "secret123",
                        null, TODAY, TODAY));
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
}
