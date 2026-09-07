package br.com.longobucco.personal_finance_app.application.usecase;

import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionResponseDto;
import br.com.longobucco.personal_finance_app.application.exception.CategoryNotFoundException;
import br.com.longobucco.personal_finance_app.application.exception.TransactionNotFoundException;
import br.com.longobucco.personal_finance_app.application.exception.UserNotFoundException;
import br.com.longobucco.personal_finance_app.application.mapper.TransactionMapper;
import br.com.longobucco.personal_finance_app.application.security.AccessGuard;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.Transaction;
import br.com.longobucco.personal_finance_app.core.domain.User;
import br.com.longobucco.personal_finance_app.core.repository.CategoryRepository;
import br.com.longobucco.personal_finance_app.core.repository.TransactionRepository;
import br.com.longobucco.personal_finance_app.core.repository.UserRepository;
import br.com.longobucco.personal_finance_app.core.exception.InvalidPeriodException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class TransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public TransactionUseCase(TransactionRepository transactionRepository, UserRepository userRepository,
                              CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
    }

    public TransactionResponseDto createTransaction(TransactionRequestDto transactionRequestDto, User requester) {
        AccessGuard.requireOwner(requester, transactionRequestDto.userId());
        User user = findUser(transactionRequestDto.userId());

        Category category = categoryRepository.findById(transactionRequestDto.categoryId())
                .filter(candidate -> candidate.isOwnedBy(user.getId()))
                .orElseThrow(() -> CategoryNotFoundException.withId(transactionRequestDto.categoryId()));

        Transaction transaction = TransactionMapper.toDomain(transactionRequestDto, category);
        Transaction savedTransaction = transactionRepository.save(transaction);
        user.applyTransaction(savedTransaction);
        userRepository.save(user);
        return TransactionMapper.toResponseDto(savedTransaction);
    }

    public TransactionResponseDto getTransactionById(UUID id, User requester) {
        return TransactionMapper.toResponseDto(findVisibleTransaction(id, requester));
    }

    public List<TransactionResponseDto> listTransactionsByUser(UUID userId, User requester) {
        AccessGuard.requireOwnerOrAdmin(requester, userId);
        requireUserExists(userId);
        return toResponseDtos(transactionRepository.findByUserId(userId));
    }

    public List<TransactionResponseDto> listTransactionsByUserBetween(UUID userId, LocalDateTime start,
                                                                       LocalDateTime end, User requester) {
        AccessGuard.requireOwnerOrAdmin(requester, userId);
        validatePeriod(start, end);
        requireUserExists(userId);
        return toResponseDtos(transactionRepository.findByUserIdAndCreatedAtBetween(userId, start, end));
    }

    public List<TransactionResponseDto> listTransactionsByUserAndTypeBetween(UUID userId, Category.Type type,
                                                                              LocalDateTime start, LocalDateTime end,
                                                                              User requester) {
        AccessGuard.requireOwnerOrAdmin(requester, userId);
        validatePeriod(start, end);
        requireUserExists(userId);
        return toResponseDtos(transactionRepository.findByUserIdAndCategoryTypeAndCreatedAtBetween(userId, type,
                start, end));
    }

    public void deleteTransaction(UUID id, User requester) {
        Transaction transaction = findOwnTransaction(id, requester);
        User user = findUser(transaction.getUserId());
        user.reverseTransaction(transaction);
        transactionRepository.delete(transaction);
        userRepository.save(user);
    }

    public TransactionResponseDto settleTransaction(UUID id, User requester) {
        Transaction transaction = findOwnTransaction(id, requester);
        transaction.settle();
        Transaction saved = transactionRepository.save(transaction);
        return TransactionMapper.toResponseDto(saved);
    }

    private Transaction findVisibleTransaction(UUID id, User requester) {
        return transactionRepository.findById(id)
                .filter(transaction -> AccessGuard.isOwnerOrAdmin(requester, transaction.getUserId()))
                .orElseThrow(() -> TransactionNotFoundException.withId(id));
    }

    private Transaction findOwnTransaction(UUID id, User requester) {
        Transaction transaction = findVisibleTransaction(id, requester);
        AccessGuard.requireOwner(requester, transaction.getUserId());
        return transaction;
    }

    private List<TransactionResponseDto> toResponseDtos(List<Transaction> transactions) {
        return transactions.stream()
                .map(TransactionMapper::toResponseDto)
                .toList();
    }

    private static void validatePeriod(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw InvalidPeriodException.nullBounds();
        }
        if (start.isAfter(end)) {
            throw InvalidPeriodException.startAfterEnd(start, end);
        }
    }

    private void requireUserExists(UUID userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw UserNotFoundException.withId(userId);
        }
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.withId(userId));
    }
}
