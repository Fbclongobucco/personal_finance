package br.com.longobucco.personal_finance_app.application.usecase;

import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionResponseDto;
import br.com.longobucco.personal_finance_app.application.exception.CategoryNotFoundException;
import br.com.longobucco.personal_finance_app.application.exception.TransactionNotFoundException;
import br.com.longobucco.personal_finance_app.application.exception.UserNotFoundException;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.Transaction;
import br.com.longobucco.personal_finance_app.core.domain.User;
import br.com.longobucco.personal_finance_app.core.repository.CategoryRepository;
import br.com.longobucco.personal_finance_app.core.repository.TransactionRepository;
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
class TransactionUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private TransactionUseCase transactionUseCase;

    @BeforeEach
    void setUp() {
        transactionUseCase = new TransactionUseCase(transactionRepository, userRepository, categoryRepository);
    }

    private User validUser() {
        return User.createUser("John Doe", "john.doe@example.com", "11987654321", "secret123",
                new BigDecimal("100.00"));
    }

    private Category validCategory() {
        return Category.createCategory("Salary", Category.Type.INCOME);
    }

    @Test
    void createTransactionSavesAndReturnsTransactionWhenUserAndCategoryExist() {
        User user = validUser();
        Category category = validCategory();
        TransactionRequestDto dto = new TransactionRequestDto("Salary", category.getId(), new BigDecimal("50.00"),
                user.getId(), Transaction.PaymentMethod.PIX);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponseDto result = transactionUseCase.createTransaction(dto);

        assertEquals("Salary", result.description());
        assertEquals(new BigDecimal("50.00"), result.amount());
        assertEquals(user.getId(), result.userId());
        assertEquals(category.getId(), result.category().id());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransactionPersistsUserWithUpdatedBalance() {
        User user = validUser();
        Category income = validCategory();
        TransactionRequestDto dto = new TransactionRequestDto("Salary", income.getId(), new BigDecimal("50.00"),
                user.getId(), Transaction.PaymentMethod.PIX);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(income.getId())).thenReturn(Optional.of(income));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transactionUseCase.createTransaction(dto);

        assertEquals(new BigDecimal("150.00"), user.getBalance());
        verify(userRepository).save(user);
    }

    @Test
    void createExpenseTransactionPersistsDecreasedUserBalance() {
        User user = validUser();
        Category expense = Category.createCategory("Rent", Category.Type.EXPENSE);
        TransactionRequestDto dto = new TransactionRequestDto("Rent", expense.getId(), new BigDecimal("30.00"),
                user.getId(), Transaction.PaymentMethod.CASH);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(expense.getId())).thenReturn(Optional.of(expense));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transactionUseCase.createTransaction(dto);

        assertEquals(new BigDecimal("70.00"), user.getBalance());
        verify(userRepository).save(user);
    }

    @Test
    void createTransactionThrowsUserNotFoundExceptionWhenUserMissing() {
        Category category = validCategory();
        UUID userId = UUID.randomUUID();
        TransactionRequestDto dto = new TransactionRequestDto("Salary", category.getId(), new BigDecimal("50.00"),
                userId, Transaction.PaymentMethod.PIX);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> transactionUseCase.createTransaction(dto));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransactionThrowsCategoryNotFoundExceptionWhenCategoryMissing() {
        User user = validUser();
        UUID categoryId = UUID.randomUUID();
        TransactionRequestDto dto = new TransactionRequestDto("Salary", categoryId, new BigDecimal("50.00"),
                user.getId(), Transaction.PaymentMethod.PIX);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> transactionUseCase.createTransaction(dto));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void getTransactionByIdReturnsTransactionWhenFound() {
        User user = validUser();
        Category category = validCategory();
        Transaction transaction = Transaction.create("Salary", category, new BigDecimal("50.00"), user,
                Transaction.PaymentMethod.PIX);
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        TransactionResponseDto result = transactionUseCase.getTransactionById(transaction.getId());

        assertEquals(transaction.getId(), result.id());
    }

    @Test
    void getTransactionByIdThrowsTransactionNotFoundExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionUseCase.getTransactionById(id));
    }

    @Test
    void listTransactionsByUserReturnsMappedTransactionsWhenUserExists() {
        User user = validUser();
        Category category = validCategory();
        Transaction transaction = Transaction.create("Salary", category, new BigDecimal("50.00"), user,
                Transaction.PaymentMethod.PIX);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(transactionRepository.findByUser(user)).thenReturn(List.of(transaction));

        List<TransactionResponseDto> result = transactionUseCase.listTransactionsByUser(user.getId());

        assertEquals(1, result.size());
        assertEquals(transaction.getId(), result.get(0).id());
    }

    @Test
    void listTransactionsByUserThrowsUserNotFoundExceptionWhenUserMissing() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> transactionUseCase.listTransactionsByUser(userId));
    }

    @Test
    void listTransactionsByUserBetweenReturnsMappedTransactions() {
        User user = validUser();
        Category category = validCategory();
        Transaction transaction = Transaction.create("Salary", category, new BigDecimal("50.00"), user,
                Transaction.PaymentMethod.PIX);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 31, 23, 59);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(transactionRepository.findByUserAndCreatedAtBetween(user, start, end)).thenReturn(List.of(transaction));

        List<TransactionResponseDto> result = transactionUseCase.listTransactionsByUserBetween(user.getId(), start, end);

        assertEquals(1, result.size());
        assertEquals(transaction.getId(), result.get(0).id());
    }

    @Test
    void listTransactionsByUserAndTypeBetweenReturnsMappedTransactions() {
        User user = validUser();
        Category category = validCategory();
        Transaction transaction = Transaction.create("Salary", category, new BigDecimal("50.00"), user,
                Transaction.PaymentMethod.PIX);
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 31, 23, 59);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(transactionRepository.findByUserAndCategoryTypeAndCreatedAtBetween(user, Category.Type.INCOME, start, end))
                .thenReturn(List.of(transaction));

        List<TransactionResponseDto> result = transactionUseCase.listTransactionsByUserAndTypeBetween(user.getId(),
                Category.Type.INCOME, start, end);

        assertEquals(1, result.size());
        assertEquals(transaction.getId(), result.get(0).id());
    }

    @Test
    void deleteTransactionDeletesTransactionWhenFound() {
        User user = validUser();
        Category category = validCategory();
        Transaction transaction = Transaction.create("Salary", category, new BigDecimal("50.00"), user,
                Transaction.PaymentMethod.PIX);
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        transactionUseCase.deleteTransaction(transaction.getId());

        verify(transactionRepository).delete(transaction);
    }

    @Test
    void deleteTransactionReversesBalanceAndPersistsUser() {
        User user = validUser();
        Category category = validCategory();
        Transaction transaction = Transaction.create("Salary", category, new BigDecimal("50.00"), user,
                Transaction.PaymentMethod.PIX);
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        transactionUseCase.deleteTransaction(transaction.getId());

        assertEquals(new BigDecimal("100.00"), user.getBalance());
        verify(userRepository).save(user);
    }

    @Test
    void deleteTransactionThrowsTransactionNotFoundExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionUseCase.deleteTransaction(id));
        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    @Test
    void settleTransactionMarksExpenseAsPaidAndSaves() {
        User user = validUser();
        Category expense = Category.createCategory("Rent", Category.Type.EXPENSE);
        Transaction transaction = Transaction.create("Rent", expense, new BigDecimal("30.00"), user,
                Transaction.PaymentMethod.CASH);
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        TransactionResponseDto result = transactionUseCase.settleTransaction(transaction.getId());

        assertEquals(transaction.getId(), result.id());
        assertTrue(result.paid());
        verify(transactionRepository).save(transaction);
    }

    @Test
    void settleTransactionThrowsTransactionNotFoundExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionUseCase.settleTransaction(id));
    }
}
