package br.com.longobucco.personal_finance_app.application.usecase;

import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionResponseDto;
import br.com.longobucco.personal_finance_app.application.exception.CategoryNotFoundException;
import br.com.longobucco.personal_finance_app.application.exception.ForbiddenException;
import br.com.longobucco.personal_finance_app.application.exception.TransactionNotFoundException;
import br.com.longobucco.personal_finance_app.application.exception.UserNotFoundException;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.Transaction;
import br.com.longobucco.personal_finance_app.core.domain.User;
import br.com.longobucco.personal_finance_app.core.repository.CategoryRepository;
import br.com.longobucco.personal_finance_app.core.repository.TransactionRepository;
import br.com.longobucco.personal_finance_app.core.exception.InvalidPeriodException;
import br.com.longobucco.personal_finance_app.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    private static final LocalDateTime JANUARY_START = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final LocalDateTime JANUARY_END = LocalDateTime.of(2026, 1, 31, 23, 59);

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private TransactionUseCase transactionUseCase;

    private User owner;
    private User intruder;
    private User admin;

    @BeforeEach
    void setUp() {
        transactionUseCase = new TransactionUseCase(transactionRepository, userRepository, categoryRepository);
        owner = User.createUser("Owner", "owner@example.com", "11987654321", "secret123", new BigDecimal("100.00"));
        intruder = User.createUser("Intruder", "intruder@example.com", "11987654321", "secret123", BigDecimal.ZERO);
        admin = User.createAdmin("Admin", "admin@example.com", "11987654321", "secret123", BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now());
    }

    private Category ownersCategory(Category.Type type) {
        return Category.createCategory(type == Category.Type.INCOME ? "Salary" : "Rent", type, owner.getId());
    }

    private Transaction ownersTransaction(Category category, BigDecimal amount) {
        return Transaction.create("Transaction", category, amount, owner.getId(), Transaction.PaymentMethod.PIX);
    }

    @Test
    void createTransactionSavesAndReturnsTransactionWhenUserAndCategoryExist() {
        Category category = ownersCategory(Category.Type.INCOME);
        TransactionRequestDto dto = new TransactionRequestDto("Salary", category.getId(), new BigDecimal("50.00"),
                owner.getId(), Transaction.PaymentMethod.PIX);
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponseDto result = transactionUseCase.createTransaction(dto, owner);

        assertEquals("Salary", result.description());
        assertEquals(new BigDecimal("50.00"), result.amount());
        assertEquals(owner.getId(), result.userId());
        assertEquals(category.getId(), result.category().id());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransactionPersistsUserWithUpdatedBalance() {
        Category income = ownersCategory(Category.Type.INCOME);
        TransactionRequestDto dto = new TransactionRequestDto("Salary", income.getId(), new BigDecimal("50.00"),
                owner.getId(), Transaction.PaymentMethod.PIX);
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(income.getId())).thenReturn(Optional.of(income));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transactionUseCase.createTransaction(dto, owner);

        assertEquals(new BigDecimal("150.00"), owner.getBalance());
        verify(userRepository).save(owner);
    }

    @Test
    void createExpenseTransactionPersistsDecreasedUserBalance() {
        Category expense = ownersCategory(Category.Type.EXPENSE);
        TransactionRequestDto dto = new TransactionRequestDto("Rent", expense.getId(), new BigDecimal("30.00"),
                owner.getId(), Transaction.PaymentMethod.CASH);
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(expense.getId())).thenReturn(Optional.of(expense));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transactionUseCase.createTransaction(dto, owner);

        assertEquals(new BigDecimal("70.00"), owner.getBalance());
        verify(userRepository).save(owner);
    }

    @Test
    void createTransactionIsForbiddenForAnAdminActingOnSomeoneElsesBehalf() {
        Category category = ownersCategory(Category.Type.INCOME);
        TransactionRequestDto dto = new TransactionRequestDto("Salary", category.getId(), new BigDecimal("50.00"),
                owner.getId(), Transaction.PaymentMethod.PIX);

        assertThrows(ForbiddenException.class, () -> transactionUseCase.createTransaction(dto, admin));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransactionThrowsUserNotFoundExceptionWhenUserMissing() {
        User ghost = User.createUser("Ghost", "ghost@example.com", "11987654321", "secret123", BigDecimal.ZERO);
        TransactionRequestDto dto = new TransactionRequestDto("Salary", UUID.randomUUID(), new BigDecimal("50.00"),
                ghost.getId(), Transaction.PaymentMethod.PIX);
        when(userRepository.findById(ghost.getId())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> transactionUseCase.createTransaction(dto, ghost));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransactionThrowsCategoryNotFoundExceptionWhenCategoryMissing() {
        UUID categoryId = UUID.randomUUID();
        TransactionRequestDto dto = new TransactionRequestDto("Salary", categoryId, new BigDecimal("50.00"),
                owner.getId(), Transaction.PaymentMethod.PIX);
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> transactionUseCase.createTransaction(dto, owner));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void createTransactionThrowsCategoryNotFoundExceptionWhenCategoryBelongsToAnotherUser() {
        Category othersCategory = Category.createCategory("Salary", Category.Type.INCOME, UUID.randomUUID());
        TransactionRequestDto dto = new TransactionRequestDto("Salary", othersCategory.getId(),
                new BigDecimal("50.00"), owner.getId(), Transaction.PaymentMethod.PIX);
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(othersCategory.getId())).thenReturn(Optional.of(othersCategory));

        assertThrows(CategoryNotFoundException.class, () -> transactionUseCase.createTransaction(dto, owner));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void getTransactionByIdReturnsTransactionForItsOwner() {
        Transaction transaction = ownersTransaction(ownersCategory(Category.Type.INCOME), new BigDecimal("50.00"));
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        TransactionResponseDto result = transactionUseCase.getTransactionById(transaction.getId(), owner);

        assertEquals(transaction.getId(), result.id());
    }

    @Test
    void getTransactionByIdReturnsTransactionForAnAdmin() {
        Transaction transaction = ownersTransaction(ownersCategory(Category.Type.INCOME), new BigDecimal("50.00"));
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        TransactionResponseDto result = transactionUseCase.getTransactionById(transaction.getId(), admin);

        assertEquals(transaction.getId(), result.id());
    }

    @Test
    void getTransactionByIdHidesAnotherUsersTransactionAsNotFound() {
        Transaction transaction = ownersTransaction(ownersCategory(Category.Type.INCOME), new BigDecimal("50.00"));
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        assertThrows(TransactionNotFoundException.class,
                () -> transactionUseCase.getTransactionById(transaction.getId(), intruder));
    }

    @Test
    void getTransactionByIdThrowsTransactionNotFoundExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionUseCase.getTransactionById(id, owner));
    }

    @Test
    void listTransactionsByUserReturnsMappedTransactionsWhenUserExists() {
        Transaction transaction = ownersTransaction(ownersCategory(Category.Type.INCOME), new BigDecimal("50.00"));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(transactionRepository.findByUserId(owner.getId())).thenReturn(List.of(transaction));

        List<TransactionResponseDto> result = transactionUseCase.listTransactionsByUser(owner.getId(), owner);

        assertEquals(1, result.size());
        assertEquals(transaction.getId(), result.get(0).id());
    }

    @Test
    void listTransactionsByUserThrowsUserNotFoundExceptionWhenUserMissing() {
        when(userRepository.findById(owner.getId())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> transactionUseCase.listTransactionsByUser(owner.getId(), owner));
    }

    @Test
    void listTransactionsByUserIsForbiddenForAnotherRegularUser() {
        assertThrows(ForbiddenException.class,
                () -> transactionUseCase.listTransactionsByUser(owner.getId(), intruder));
        verify(transactionRepository, never()).findByUserId(any(UUID.class));
    }

    @Test
    void listTransactionsByUserBetweenReturnsMappedTransactions() {
        Transaction transaction = ownersTransaction(ownersCategory(Category.Type.INCOME), new BigDecimal("50.00"));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(transactionRepository.findByUserIdAndCreatedAtBetween(owner.getId(), JANUARY_START, JANUARY_END)).thenReturn(List.of(transaction));

        List<TransactionResponseDto> result = transactionUseCase.listTransactionsByUserBetween(owner.getId(),
                JANUARY_START, JANUARY_END, owner);

        assertEquals(1, result.size());
        assertEquals(transaction.getId(), result.get(0).id());
    }

    @Test
    void listTransactionsByUserAndTypeBetweenReturnsMappedTransactions() {
        Transaction transaction = ownersTransaction(ownersCategory(Category.Type.INCOME), new BigDecimal("50.00"));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(transactionRepository.findByUserIdAndCategoryTypeAndCreatedAtBetween(owner.getId(), Category.Type.INCOME, JANUARY_START, JANUARY_END))
                .thenReturn(List.of(transaction));

        List<TransactionResponseDto> result = transactionUseCase.listTransactionsByUserAndTypeBetween(owner.getId(),
                Category.Type.INCOME, JANUARY_START, JANUARY_END, owner);

        assertEquals(1, result.size());
        assertEquals(transaction.getId(), result.get(0).id());
    }

    @Test
    void listTransactionsByUserBetweenThrowsWhenStartIsAfterEnd() {
        assertThrows(InvalidPeriodException.class, () -> transactionUseCase.listTransactionsByUserBetween(
                owner.getId(), JANUARY_END, JANUARY_START, owner));
        verify(transactionRepository, never()).findByUserIdAndCreatedAtBetween(any(UUID.class),
                any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void listTransactionsByUserBetweenThrowsWhenABoundIsNull() {
        assertThrows(InvalidPeriodException.class, () -> transactionUseCase.listTransactionsByUserBetween(
                owner.getId(), JANUARY_START, null, owner));
    }

    @Test
    void listTransactionsByUserAndTypeBetweenThrowsWhenStartIsAfterEnd() {
        assertThrows(InvalidPeriodException.class, () -> transactionUseCase.listTransactionsByUserAndTypeBetween(
                owner.getId(), Category.Type.INCOME, JANUARY_END, JANUARY_START, owner));
    }

    @Test
    void deleteTransactionDeletesTransactionWhenFound() {
        Transaction transaction = ownersTransaction(ownersCategory(Category.Type.INCOME), new BigDecimal("50.00"));
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        transactionUseCase.deleteTransaction(transaction.getId(), owner);

        verify(transactionRepository).delete(transaction);
    }

    @Test
    void deleteTransactionReversesBalanceAndPersistsUser() {
        Category income = ownersCategory(Category.Type.INCOME);
        Transaction transaction = ownersTransaction(income, new BigDecimal("50.00"));
        owner.applyTransaction(transaction);
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

        transactionUseCase.deleteTransaction(transaction.getId(), owner);

        assertEquals(new BigDecimal("100.00"), owner.getBalance());
        verify(userRepository).save(owner);
    }

    @Test
    void deleteTransactionIsForbiddenForAnAdminWhoIsNotTheOwner() {
        Transaction transaction = ownersTransaction(ownersCategory(Category.Type.INCOME), new BigDecimal("50.00"));
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        assertThrows(ForbiddenException.class,
                () -> transactionUseCase.deleteTransaction(transaction.getId(), admin));
        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    @Test
    void deleteTransactionThrowsTransactionNotFoundExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionUseCase.deleteTransaction(id, owner));
        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    @Test
    void settleTransactionMarksExpenseAsPaidAndSaves() {
        Transaction transaction = Transaction.create("Rent", ownersCategory(Category.Type.EXPENSE),
                new BigDecimal("30.00"), owner.getId(), Transaction.PaymentMethod.CASH);
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        TransactionResponseDto result = transactionUseCase.settleTransaction(transaction.getId(), owner);

        assertEquals(transaction.getId(), result.id());
        assertTrue(result.paid());
        verify(transactionRepository).save(transaction);
    }

    @Test
    void settleTransactionLeavesTheProjectedBalanceUntouched() {
        Transaction transaction = Transaction.create("Rent", ownersCategory(Category.Type.EXPENSE),
                new BigDecimal("30.00"), owner.getId(), Transaction.PaymentMethod.CASH);
        owner.applyTransaction(transaction);
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(transaction)).thenReturn(transaction);

        transactionUseCase.settleTransaction(transaction.getId(), owner);

        assertEquals(new BigDecimal("70.00"), owner.getBalance());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void settleTransactionIsForbiddenForAnAdminWhoIsNotTheOwner() {
        Transaction transaction = Transaction.create("Rent", ownersCategory(Category.Type.EXPENSE),
                new BigDecimal("30.00"), owner.getId(), Transaction.PaymentMethod.CASH);
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        assertThrows(ForbiddenException.class,
                () -> transactionUseCase.settleTransaction(transaction.getId(), admin));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void settleTransactionThrowsTransactionNotFoundExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionUseCase.settleTransaction(id, owner));
    }
}
