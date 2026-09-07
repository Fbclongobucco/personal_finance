package br.com.longobucco.personal_finance_app.application.usecase;

import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionResponseDto;
import br.com.longobucco.personal_finance_app.application.dto.user.UserRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.user.UserResponseDto;
import br.com.longobucco.personal_finance_app.application.exception.UserAlreadyExistsException;
import br.com.longobucco.personal_finance_app.application.exception.UserNotFoundException;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.Transaction;
import br.com.longobucco.personal_finance_app.core.domain.User;
import br.com.longobucco.personal_finance_app.core.repository.TransactionRepository;
import br.com.longobucco.personal_finance_app.core.repository.UserRepository;
import br.com.longobucco.personal_finance_app.core.security.PasswordHasher;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PasswordHasher passwordHasher;

    private UserUseCase userUseCase;

    @BeforeEach
    void setUp() {
        userUseCase = new UserUseCase(userRepository, transactionRepository, passwordHasher);
    }

    private User validUser() {
        return User.createUser("John Doe", "john.doe@example.com", "11987654321", "secret123",
                new BigDecimal("100.00"));
    }

    private UserRequestDto validRequestDto() {
        return new UserRequestDto("John Doe", "john.doe@example.com", "secret123", "11987654321",
                new BigDecimal("100.00"));
    }

    private Transaction transactionFor(User user, Category category, BigDecimal amount, LocalDateTime date) {
        return Transaction.recover(UUID.randomUUID(), "Transaction", category, amount, user, date, date,
                Transaction.PaymentMethod.CASH);
    }

    @Test
    void createUserSavesAndReturnsUserWhenEmailNotInUse() {
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(passwordHasher.hash("secret123")).thenReturn("hashed:secret123");
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
    void getUserTransactionsReturnsAllMappedTransactionsFromRepository() {
        User user = validUser();
        Category category = Category.createCategory("Salary", Category.Type.INCOME);
        Transaction transaction = transactionFor(user, category, new BigDecimal("50.00"),
                LocalDateTime.of(2026, 1, 15, 10, 0));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(transactionRepository.findByUser(user)).thenReturn(List.of(transaction));

        List<TransactionResponseDto> result = userUseCase.getUserTransactions(user.getId());

        assertEquals(1, result.size());
        assertEquals(transaction.getId(), result.get(0).id());
    }

    @Test
    void getUserTransactionsBetweenDelegatesToTransactionRepository() {
        User user = validUser();
        Category category = Category.createCategory("Rent", Category.Type.EXPENSE);
        Transaction inRange = transactionFor(user, category, new BigDecimal("30.00"),
                LocalDateTime.of(2026, 1, 15, 10, 0));
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 31, 23, 59);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(transactionRepository.findByUserAndCreatedAtBetween(user, start, end)).thenReturn(List.of(inRange));

        List<TransactionResponseDto> result = userUseCase.getUserTransactionsBetween(user.getId(), start, end);

        assertEquals(1, result.size());
        assertEquals(inRange.getId(), result.get(0).id());
    }

    @Test
    void getUserTransactionsByTypeBetweenDelegatesToTransactionRepository() {
        User user = validUser();
        Category income = Category.createCategory("Salary", Category.Type.INCOME);
        LocalDateTime date = LocalDateTime.of(2026, 1, 15, 10, 0);
        Transaction salary = transactionFor(user, income, new BigDecimal("1000.00"), date);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 31, 23, 59);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(transactionRepository.findByUserAndCategoryTypeAndCreatedAtBetween(user, Category.Type.INCOME, start, end))
                .thenReturn(List.of(salary));

        List<TransactionResponseDto> result = userUseCase.getUserTransactionsByTypeBetween(user.getId(),
                Category.Type.INCOME, start, end);

        assertEquals(1, result.size());
        assertEquals(salary.getId(), result.get(0).id());
    }

    @Test
    void getUserExpensesBetweenDelegatesToTransactionRepositoryWithExpenseType() {
        User user = validUser();
        Category expense = Category.createCategory("Rent", Category.Type.EXPENSE);
        LocalDateTime date = LocalDateTime.of(2026, 1, 15, 10, 0);
        Transaction rent = transactionFor(user, expense, new BigDecimal("30.00"), date);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 31, 23, 59);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(transactionRepository.findByUserAndCategoryTypeAndCreatedAtBetween(user, Category.Type.EXPENSE, start, end))
                .thenReturn(List.of(rent));

        List<TransactionResponseDto> result = userUseCase.getUserExpensesBetween(user.getId(), start, end);

        assertEquals(1, result.size());
        assertEquals(rent.getId(), result.get(0).id());
    }

    @Test
    void getUserIncomesBetweenDelegatesToTransactionRepositoryWithIncomeType() {
        User user = validUser();
        Category income = Category.createCategory("Salary", Category.Type.INCOME);
        LocalDateTime date = LocalDateTime.of(2026, 1, 15, 10, 0);
        Transaction salary = transactionFor(user, income, new BigDecimal("1000.00"), date);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 31, 23, 59);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(transactionRepository.findByUserAndCategoryTypeAndCreatedAtBetween(user, Category.Type.INCOME, start, end))
                .thenReturn(List.of(salary));

        List<TransactionResponseDto> result = userUseCase.getUserIncomesBetween(user.getId(), start, end);

        assertEquals(1, result.size());
        assertEquals(salary.getId(), result.get(0).id());
    }
}
