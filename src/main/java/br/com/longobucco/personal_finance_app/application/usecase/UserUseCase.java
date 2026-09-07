package br.com.longobucco.personal_finance_app.application.usecase;

import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionResponseDto;
import br.com.longobucco.personal_finance_app.application.dto.user.UserRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.user.UserResponseDto;
import br.com.longobucco.personal_finance_app.application.exception.UserAlreadyExistsException;
import br.com.longobucco.personal_finance_app.application.exception.UserNotFoundException;
import br.com.longobucco.personal_finance_app.application.mapper.TransactionMapper;
import br.com.longobucco.personal_finance_app.application.mapper.UserMapper;
import br.com.longobucco.personal_finance_app.application.security.AccessGuard;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.Transaction;
import br.com.longobucco.personal_finance_app.core.domain.User;
import br.com.longobucco.personal_finance_app.core.repository.CategoryRepository;
import br.com.longobucco.personal_finance_app.core.repository.TransactionRepository;
import br.com.longobucco.personal_finance_app.core.repository.UserRepository;
import br.com.longobucco.personal_finance_app.core.security.PasswordHasher;
import br.com.longobucco.personal_finance_app.core.exception.InvalidPeriodException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class UserUseCase {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordHasher passwordHasher;

    public UserUseCase(UserRepository userRepository, TransactionRepository transactionRepository,
                       CategoryRepository categoryRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.passwordHasher = passwordHasher;
    }

    public UserResponseDto createUser(UserRequestDto userRequestDto) {
        if (userRepository.existsByEmail(userRequestDto.email())) {
            throw UserAlreadyExistsException.withEmail(userRequestDto.email());
        }
        UserRequestDto withHashedPassword = new UserRequestDto(userRequestDto.name(), userRequestDto.email(),
                passwordHasher.hash(userRequestDto.password()), userRequestDto.phone(),
                userRequestDto.initialBalance());
        User user = UserMapper.toDomain(withHashedPassword);
        User savedUser = userRepository.save(user);
        seedDefaultCategories(savedUser);

        return UserMapper.toResponseDto(savedUser, BigDecimal.ZERO);
    }

    public UserResponseDto getUserById(UUID id, User requester) {
        AccessGuard.requireOwnerOrAdmin(requester, id);
        return toResponseDto(findUser(id));
    }

    public UserResponseDto getUserByEmail(String email, User requester) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> UserNotFoundException.withEmail(email));
        AccessGuard.requireOwnerOrAdmin(requester, user.getId());
        return toResponseDto(user);
    }

    public void deleteUser(UUID id, User requester) {
        AccessGuard.requireOwnerOrAdmin(requester, id);
        userRepository.delete(findUser(id));
    }

    public List<TransactionResponseDto> getUserTransactions(UUID id, User requester) {
        AccessGuard.requireOwnerOrAdmin(requester, id);
        findUser(id);
        return toResponseDtos(transactionRepository.findByUserId(id));
    }

    public List<TransactionResponseDto> getUserTransactionsBetween(UUID id, LocalDateTime start, LocalDateTime end,
                                                                    User requester) {
        AccessGuard.requireOwnerOrAdmin(requester, id);
        validatePeriod(start, end);
        findUser(id);
        return toResponseDtos(transactionRepository.findByUserIdAndCreatedAtBetween(id, start, end));
    }

    public List<TransactionResponseDto> getUserTransactionsByTypeBetween(UUID id, Category.Type type,
                                                                          LocalDateTime start, LocalDateTime end,
                                                                          User requester) {
        AccessGuard.requireOwnerOrAdmin(requester, id);
        validatePeriod(start, end);
        findUser(id);
        return toResponseDtos(transactionRepository.findByUserIdAndCategoryTypeAndCreatedAtBetween(id, type, start,
                end));
    }

    public List<TransactionResponseDto> getUserExpensesBetween(UUID id, LocalDateTime start, LocalDateTime end,
                                                               User requester) {
        return getUserTransactionsByTypeBetween(id, Category.Type.EXPENSE, start, end, requester);
    }

    public List<TransactionResponseDto> getUserIncomesBetween(UUID id, LocalDateTime start, LocalDateTime end,
                                                              User requester) {
        return getUserTransactionsByTypeBetween(id, Category.Type.INCOME, start, end, requester);
    }

    private static void validatePeriod(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw InvalidPeriodException.nullBounds();
        }
        if (start.isAfter(end)) {
            throw InvalidPeriodException.startAfterEnd(start, end);
        }
    }

    private void seedDefaultCategories(User user) {
        Category.defaultsFor(user.getId()).forEach(categoryRepository::save);
    }

    private UserResponseDto toResponseDto(User user) {
        return UserMapper.toResponseDto(user, transactionRepository.sumPendingExpenses(user.getId()));
    }

    private List<TransactionResponseDto> toResponseDtos(List<Transaction> transactions) {
        return transactions.stream()
                .map(TransactionMapper::toResponseDto)
                .toList();
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> UserNotFoundException.withId(id));
    }
}
