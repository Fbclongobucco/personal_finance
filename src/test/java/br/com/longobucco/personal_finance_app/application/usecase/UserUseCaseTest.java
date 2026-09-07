package br.com.longobucco.personal_finance_app.application.usecase;

import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionResponseDto;
import br.com.longobucco.personal_finance_app.application.dto.user.UserRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.user.UserResponseDto;
import br.com.longobucco.personal_finance_app.application.exception.UserAlreadyExistsException;
import br.com.longobucco.personal_finance_app.application.exception.UserNotFoundException;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.Transaction;
import br.com.longobucco.personal_finance_app.core.domain.User;
import br.com.longobucco.personal_finance_app.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    private UserUseCase userUseCase;

    @BeforeEach
    void setUp() {
        userUseCase = new UserUseCase(userRepository);
    }

    private User validUser() {
        return User.createUser("John Doe", "john.doe@example.com", "11987654321", "secret123",
                new BigDecimal("100.00"));
    }

    private UserRequestDto validRequestDto() {
        return new UserRequestDto("John Doe", "john.doe@example.com", "secret123", "11987654321",
                new BigDecimal("100.00"));
    }

    @Test
    void createUserSavesAndReturnsUserWhenEmailNotInUse() {
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDto result = userUseCase.createUser(validRequestDto());

        assertEquals("John Doe", result.name());
        assertEquals("john.doe@example.com", result.email());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUserThrowsUserAlreadyExistsExceptionWhenEmailInUse() {
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userUseCase.createUser(validRequestDto()));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserByIdReturnsUserWhenFound() {
        User user = validUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UserResponseDto result = userUseCase.getUserById(user.getId());

        assertEquals(user.getId(), result.id());
        assertEquals(user.getEmail(), result.email());
    }

    @Test
    void getUserByIdThrowsUserNotFoundExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userUseCase.getUserById(id));
    }

    @Test
    void getUserByEmailReturnsUserWhenFound() {
        User user = validUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        UserResponseDto result = userUseCase.getUserByEmail(user.getEmail());

        assertEquals(user.getId(), result.id());
    }

    @Test
    void getUserByEmailThrowsUserNotFoundExceptionWhenNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userUseCase.getUserByEmail("missing@example.com"));
    }

    @Test
    void deleteUserDeletesUserWhenFound() {
        User user = validUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        userUseCase.deleteUser(user.getId());

        verify(userRepository).delete(user);
    }

    @Test
    void deleteUserThrowsUserNotFoundExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userUseCase.deleteUser(id));
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void getUserTransactionsReturnsAllMappedTransactions() {
        User user = validUser();
        Category category = Category.createCategory("Salary", Category.Type.INCOME);
        Transaction transaction = Transaction.create("Salary", category, new BigDecimal("50.00"), user,
                Transaction.PaymentMethod.PIX);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        List<TransactionResponseDto> result = userUseCase.getUserTransactions(user.getId());

        assertEquals(1, result.size());
        assertEquals(transaction.getId(), result.get(0).id());
    }

    @Test
    void getUserTransactionsBetweenReturnsOnlyTransactionsWithinRange() {
        User user = validUser();
        Category category = Category.createCategory("Rent", Category.Type.EXPENSE);
        Transaction inRange = Transaction.recover(UUID.randomUUID(), "Rent", category, new BigDecimal("30.00"),
                user, LocalDateTime.of(2026, 1, 15, 10, 0), LocalDateTime.of(2026, 1, 15, 10, 0),
                Transaction.PaymentMethod.CASH);
        Transaction.recover(UUID.randomUUID(), "Rent", category, new BigDecimal("40.00"), user,
                LocalDateTime.of(2026, 3, 1, 10, 0), LocalDateTime.of(2026, 3, 1, 10, 0),
                Transaction.PaymentMethod.CASH);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        List<TransactionResponseDto> result = userUseCase.getUserTransactionsBetween(user.getId(),
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 31, 23, 59));

        assertEquals(1, result.size());
        assertEquals(inRange.getId(), result.get(0).id());
    }

    @Test
    void getUserTransactionsByTypeBetweenReturnsOnlyMatchingType() {
        User user = validUser();
        Category income = Category.createCategory("Salary", Category.Type.INCOME);
        Category expense = Category.createCategory("Rent", Category.Type.EXPENSE);
        LocalDateTime date = LocalDateTime.of(2026, 1, 15, 10, 0);
        Transaction salary = Transaction.recover(UUID.randomUUID(), "Salary", income, new BigDecimal("1000.00"),
                user, date, date, Transaction.PaymentMethod.PIX);
        Transaction.recover(UUID.randomUUID(), "Rent", expense, new BigDecimal("30.00"), user, date, date,
                Transaction.PaymentMethod.CASH);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        List<TransactionResponseDto> result = userUseCase.getUserTransactionsByTypeBetween(user.getId(),
                Category.Type.INCOME, LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 31, 23, 59));

        assertEquals(1, result.size());
        assertEquals(salary.getId(), result.get(0).id());
    }

    @Test
    void getUserExpensesBetweenReturnsOnlyExpenses() {
        User user = validUser();
        Category income = Category.createCategory("Salary", Category.Type.INCOME);
        Category expense = Category.createCategory("Rent", Category.Type.EXPENSE);
        LocalDateTime date = LocalDateTime.of(2026, 1, 15, 10, 0);
        Transaction.recover(UUID.randomUUID(), "Salary", income, new BigDecimal("1000.00"), user, date, date,
                Transaction.PaymentMethod.PIX);
        Transaction rent = Transaction.recover(UUID.randomUUID(), "Rent", expense, new BigDecimal("30.00"), user,
                date, date, Transaction.PaymentMethod.CASH);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        List<TransactionResponseDto> result = userUseCase.getUserExpensesBetween(user.getId(),
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 31, 23, 59));

        assertEquals(1, result.size());
        assertEquals(rent.getId(), result.get(0).id());
    }

    @Test
    void getUserIncomesBetweenReturnsOnlyIncomes() {
        User user = validUser();
        Category income = Category.createCategory("Salary", Category.Type.INCOME);
        Category expense = Category.createCategory("Rent", Category.Type.EXPENSE);
        LocalDateTime date = LocalDateTime.of(2026, 1, 15, 10, 0);
        Transaction salary = Transaction.recover(UUID.randomUUID(), "Salary", income, new BigDecimal("1000.00"),
                user, date, date, Transaction.PaymentMethod.PIX);
        Transaction.recover(UUID.randomUUID(), "Rent", expense, new BigDecimal("30.00"), user, date, date,
                Transaction.PaymentMethod.CASH);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        List<TransactionResponseDto> result = userUseCase.getUserIncomesBetween(user.getId(),
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 31, 23, 59));

        assertEquals(1, result.size());
        assertEquals(salary.getId(), result.get(0).id());
        assertTrue(result.stream().noneMatch(dto -> dto.category().type() == Category.Type.EXPENSE));
    }
}
