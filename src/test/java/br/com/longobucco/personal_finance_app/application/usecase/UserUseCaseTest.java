package br.com.longobucco.personal_finance_app.application.usecase;

import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionResponseDto;
import br.com.longobucco.personal_finance_app.application.dto.user.UserRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.user.UserResponseDto;
import br.com.longobucco.personal_finance_app.application.exception.ForbiddenException;
import br.com.longobucco.personal_finance_app.application.exception.UserAlreadyExistsException;
import br.com.longobucco.personal_finance_app.application.exception.UserNotFoundException;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.Transaction;
import br.com.longobucco.personal_finance_app.core.domain.User;
import br.com.longobucco.personal_finance_app.core.repository.CategoryRepository;
import br.com.longobucco.personal_finance_app.core.repository.TransactionRepository;
import br.com.longobucco.personal_finance_app.core.exception.InvalidPeriodException;
import br.com.longobucco.personal_finance_app.core.repository.UserRepository;
import br.com.longobucco.personal_finance_app.core.security.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserUseCaseTest {

    private static final LocalDateTime JANUARY_START = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final LocalDateTime JANUARY_END = LocalDateTime.of(2026, 1, 31, 23, 59);

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private PasswordHasher passwordHasher;

    private UserUseCase userUseCase;

    private User admin;

    @BeforeEach
    void setUp() {
        userUseCase = new UserUseCase(userRepository, transactionRepository, categoryRepository, passwordHasher);
        admin = User.createAdmin("Admin", "admin@example.com", "11987654321", "secret123", BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now());
    }

    private User validUser() {
        return User.createUser("John Doe", "john.doe@example.com", "11987654321", "secret123",
                new BigDecimal("100.00"));
    }

    private User intruder() {
        return User.createUser("Intruder", "intruder@example.com", "11987654321", "secret123", BigDecimal.ZERO);
    }

    private UserRequestDto validRequestDto() {
        return new UserRequestDto("John Doe", "john.doe@example.com", "secret123", "11987654321",
                new BigDecimal("100.00"));
    }

    private Transaction transactionFor(User user, Category category, BigDecimal amount, LocalDateTime date) {
        return Transaction.recover(UUID.randomUUID(), "Transaction", category, amount, user.getId(), date, date,
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
        assertEquals(new BigDecimal("100.00"), result.balance());
        assertEquals(new BigDecimal("100.00"), result.settledBalance());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUserGivesTheNewAccountItsDefaultCategories() {
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(passwordHasher.hash("secret123")).thenReturn("hashed:secret123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDto result = userUseCase.createUser(validRequestDto());

        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository, atLeastOnce()).save(saved.capture());
        assertFalse(saved.getAllValues().isEmpty());
        assertTrue(saved.getAllValues().stream().allMatch(category -> category.isOwnedBy(result.id())));
    }

    @Test
    void createUserThrowsUserAlreadyExistsExceptionWhenEmailInUse() {
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userUseCase.createUser(validRequestDto()));
        verify(userRepository, never()).save(any(User.class));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void getUserByIdReturnsUserWhenFound() {
        User user = validUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(transactionRepository.sumPendingExpenses(user.getId())).thenReturn(BigDecimal.ZERO);

        UserResponseDto result = userUseCase.getUserById(user.getId(), user);

        assertEquals(user.getId(), result.id());
        assertEquals(user.getEmail(), result.email());
    }

    @Test
    void getUserByIdReportsTheSettledBalanceApartFromTheProjectedOne() {
        User user = validUser();
        Category expense = Category.createCategory("Rent", Category.Type.EXPENSE, user.getId());
        user.applyTransaction(transactionFor(user, expense, new BigDecimal("30.00"), LocalDateTime.now()));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(transactionRepository.sumPendingExpenses(user.getId())).thenReturn(new BigDecimal("30.00"));

        UserResponseDto result = userUseCase.getUserById(user.getId(), user);

        assertEquals(new BigDecimal("70.00"), result.balance());
        assertEquals(new BigDecimal("100.00"), result.settledBalance());
    }

    @Test
    void getUserByIdIsForbiddenForAnotherRegularUser() {
        User user = validUser();

        assertThrows(ForbiddenException.class, () -> userUseCase.getUserById(user.getId(), intruder()));
        verify(userRepository, never()).findById(any(UUID.class));
    }

    @Test
    void getUserByIdThrowsUserNotFoundExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userUseCase.getUserById(id, admin));
    }

    @Test
    void getUserByEmailReturnsUserWhenFound() {
        User user = validUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(transactionRepository.sumPendingExpenses(user.getId())).thenReturn(BigDecimal.ZERO);

        UserResponseDto result = userUseCase.getUserByEmail(user.getEmail(), user);

        assertEquals(user.getId(), result.id());
    }

    @Test
    void getUserByEmailIsForbiddenForAnotherRegularUser() {
        User user = validUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThrows(ForbiddenException.class, () -> userUseCase.getUserByEmail(user.getEmail(), intruder()));
    }

    @Test
    void getUserByEmailThrowsUserNotFoundExceptionWhenNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userUseCase.getUserByEmail("missing@example.com", admin));
    }

    @Test
    void deleteUserDeletesUserWhenFound() {
        User user = validUser();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        userUseCase.deleteUser(user.getId(), user);

        verify(userRepository).delete(user);
    }

    @Test
    void deleteUserIsForbiddenForAnotherRegularUser() {
        User user = validUser();

        assertThrows(ForbiddenException.class, () -> userUseCase.deleteUser(user.getId(), intruder()));
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void deleteUserThrowsUserNotFoundExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userUseCase.deleteUser(id, admin));
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    void getUserTransactionsReturnsAllMappedTransactionsFromRepository() {
        User user = validUser();
        Category category = Category.createCategory("Salary", Category.Type.INCOME, user.getId());
        Transaction transaction = transactionFor(user, category, new BigDecimal("50.00"),
                LocalDateTime.of(2026, 1, 15, 10, 0));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(transactionRepository.findByUserId(user.getId())).thenReturn(List.of(transaction));

        List<TransactionResponseDto> result = userUseCase.getUserTransactions(user.getId(), user);

        assertEquals(1, result.size());
        assertEquals(transaction.getId(), result.get(0).id());
    }

    @Test
    void getUserTransactionsIsForbiddenForAnotherRegularUser() {
        User user = validUser();

        assertThrows(ForbiddenException.class, () -> userUseCase.getUserTransactions(user.getId(), intruder()));
        verify(transactionRepository, never()).findByUserId(any(UUID.class));
    }

    @Test
    void getUserTransactionsBetweenDelegatesToTransactionRepository() {
        User user = validUser();
        Category category = Category.createCategory("Rent", Category.Type.EXPENSE, user.getId());
        Transaction inRange = transactionFor(user, category, new BigDecimal("30.00"),
                LocalDateTime.of(2026, 1, 15, 10, 0));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(transactionRepository.findByUserIdAndCreatedAtBetween(user.getId(), JANUARY_START, JANUARY_END)).thenReturn(List.of(inRange));

        List<TransactionResponseDto> result = userUseCase.getUserTransactionsBetween(user.getId(), JANUARY_START, JANUARY_END, user);

        assertEquals(1, result.size());
        assertEquals(inRange.getId(), result.get(0).id());
    }

    @Test
    void getUserTransactionsBetweenThrowsWhenStartIsAfterEnd() {
        User user = validUser();

        assertThrows(InvalidPeriodException.class, () -> userUseCase.getUserTransactionsBetween(
                user.getId(), JANUARY_END, JANUARY_START, user));
        verify(transactionRepository, never()).findByUserIdAndCreatedAtBetween(any(UUID.class),
                any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void getUserTransactionsBetweenThrowsWhenABoundIsNull() {
        User user = validUser();

        assertThrows(InvalidPeriodException.class, () -> userUseCase.getUserTransactionsBetween(
                user.getId(), null, JANUARY_END, user));
    }

    @Test
    void getUserTransactionsByTypeBetweenThrowsWhenStartIsAfterEnd() {
        User user = validUser();

        assertThrows(InvalidPeriodException.class, () -> userUseCase.getUserTransactionsByTypeBetween(
                user.getId(), Category.Type.INCOME, JANUARY_END, JANUARY_START, user));
    }

    @Test
    void getUserTransactionsByTypeBetweenDelegatesToTransactionRepository() {
        User user = validUser();
        Category income = Category.createCategory("Salary", Category.Type.INCOME, user.getId());
        Transaction salary = transactionFor(user, income, new BigDecimal("1000.00"),
                LocalDateTime.of(2026, 1, 15, 10, 0));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(transactionRepository.findByUserIdAndCategoryTypeAndCreatedAtBetween(user.getId(), Category.Type.INCOME, JANUARY_START, JANUARY_END))
                .thenReturn(List.of(salary));

        List<TransactionResponseDto> result = userUseCase.getUserTransactionsByTypeBetween(user.getId(),
                Category.Type.INCOME, JANUARY_START, JANUARY_END, user);

        assertEquals(1, result.size());
        assertEquals(salary.getId(), result.get(0).id());
    }

    @Test
    void getUserExpensesBetweenDelegatesToTransactionRepositoryWithExpenseType() {
        User user = validUser();
        Category expense = Category.createCategory("Rent", Category.Type.EXPENSE, user.getId());
        Transaction rent = transactionFor(user, expense, new BigDecimal("30.00"),
                LocalDateTime.of(2026, 1, 15, 10, 0));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(transactionRepository.findByUserIdAndCategoryTypeAndCreatedAtBetween(user.getId(), Category.Type.EXPENSE, JANUARY_START, JANUARY_END))
                .thenReturn(List.of(rent));

        List<TransactionResponseDto> result = userUseCase.getUserExpensesBetween(user.getId(), JANUARY_START, JANUARY_END, user);

        assertEquals(1, result.size());
        assertEquals(rent.getId(), result.get(0).id());
    }

    @Test
    void getUserIncomesBetweenDelegatesToTransactionRepositoryWithIncomeType() {
        User user = validUser();
        Category income = Category.createCategory("Salary", Category.Type.INCOME, user.getId());
        Transaction salary = transactionFor(user, income, new BigDecimal("1000.00"),
                LocalDateTime.of(2026, 1, 15, 10, 0));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(transactionRepository.findByUserIdAndCategoryTypeAndCreatedAtBetween(user.getId(), Category.Type.INCOME, JANUARY_START, JANUARY_END))
                .thenReturn(List.of(salary));

        List<TransactionResponseDto> result = userUseCase.getUserIncomesBetween(user.getId(), JANUARY_START, JANUARY_END, user);

        assertEquals(1, result.size());
        assertEquals(salary.getId(), result.get(0).id());
    }
}
