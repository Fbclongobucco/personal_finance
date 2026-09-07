package br.com.longobucco.personal_finance_app.application.usecase;

import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionResponseDto;
import br.com.longobucco.personal_finance_app.application.exception.CategoryNotFoundException;
import br.com.longobucco.personal_finance_app.application.exception.TransactionNotFoundException;
import br.com.longobucco.personal_finance_app.application.exception.UserNotFoundException;
import br.com.longobucco.personal_finance_app.application.mapper.TransactionMapper;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.Transaction;
import br.com.longobucco.personal_finance_app.core.domain.User;
import br.com.longobucco.personal_finance_app.core.repository.CategoryRepository;
import br.com.longobucco.personal_finance_app.core.repository.TransactionRepository;
import br.com.longobucco.personal_finance_app.core.repository.UserRepository;

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

    public TransactionResponseDto createTransaction(TransactionRequestDto transactionRequestDto) {
        User user = userRepository.findById(transactionRequestDto.userId())
                .orElseThrow(() -> UserNotFoundException.withId(transactionRequestDto.userId()));
        Category category = categoryRepository.findById(transactionRequestDto.categoryId())
                .orElseThrow(() -> CategoryNotFoundException.withId(transactionRequestDto.categoryId()));

        Transaction transaction = TransactionMapper.toDomain(transactionRequestDto, user, category);
        Transaction savedTransaction = transactionRepository.save(transaction);
        userRepository.save(user);
        return TransactionMapper.toResponseDto(savedTransaction);
    }

    public TransactionResponseDto getTransactionById(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> TransactionNotFoundException.withId(id));
        return TransactionMapper.toResponseDto(transaction);
    }

    public List<TransactionResponseDto> listTransactionsByUser(UUID userId) {
        User user = findUser(userId);
        return transactionRepository.findByUser(user).stream()
                .map(TransactionMapper::toResponseDto)
                .toList();
    }

    public List<TransactionResponseDto> listTransactionsByUserBetween(UUID userId, LocalDateTime start,
                                                                       LocalDateTime end) {
        User user = findUser(userId);
        return transactionRepository.findByUserAndCreatedAtBetween(user, start, end).stream()
                .map(TransactionMapper::toResponseDto)
                .toList();
    }

    public List<TransactionResponseDto> listTransactionsByUserAndTypeBetween(UUID userId, Category.Type type,
                                                                              LocalDateTime start, LocalDateTime end) {
        User user = findUser(userId);
        return transactionRepository.findByUserAndCategoryTypeAndCreatedAtBetween(user, type, start, end).stream()
                .map(TransactionMapper::toResponseDto)
                .toList();
    }

    public void deleteTransaction(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> TransactionNotFoundException.withId(id));
        transaction.getUser().removeTransaction(transaction);
        transactionRepository.delete(transaction);
        userRepository.save(transaction.getUser());
    }

    /** Marks an EXPENSE transaction as settled ("dar baixa"). */
    public TransactionResponseDto settleTransaction(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> TransactionNotFoundException.withId(id));
        transaction.settle();
        Transaction saved = transactionRepository.save(transaction);
        return TransactionMapper.toResponseDto(saved);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> UserNotFoundException.withId(userId));
    }
}
