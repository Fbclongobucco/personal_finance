package br.com.longobucco.personal_finance_app.application.usecase;

import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionResponseDto;
import br.com.longobucco.personal_finance_app.application.dto.user.UserRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.user.UserResponseDto;
import br.com.longobucco.personal_finance_app.application.exception.UserAlreadyExistsException;
import br.com.longobucco.personal_finance_app.application.exception.UserNotFoundException;
import br.com.longobucco.personal_finance_app.application.mapper.TransactionMapper;
import br.com.longobucco.personal_finance_app.application.mapper.UserMapper;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.User;
import br.com.longobucco.personal_finance_app.core.repository.TransactionRepository;
import br.com.longobucco.personal_finance_app.core.repository.UserRepository;
import br.com.longobucco.personal_finance_app.core.security.PasswordHasher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class UserUseCase {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordHasher passwordHasher;

    public UserUseCase(UserRepository userRepository, TransactionRepository transactionRepository,
                       PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
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
        return UserMapper.toResponseDto(savedUser);
    }

    public UserResponseDto getUserById(UUID id) {
        return UserMapper.toResponseDto(findUser(id));
    }

    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> UserNotFoundException.withEmail(email));
        return UserMapper.toResponseDto(user);
    }

    public void deleteUser(UUID id) {
        userRepository.delete(findUser(id));
    }

    public List<TransactionResponseDto> getUserTransactions(UUID id) {
        return transactionRepository.findByUser(findUser(id)).stream()
                .map(TransactionMapper::toResponseDto)
                .toList();
    }

    public List<TransactionResponseDto> getUserTransactionsBetween(UUID id, LocalDateTime start, LocalDateTime end) {
        return transactionRepository.findByUserAndCreatedAtBetween(findUser(id), start, end).stream()
                .map(TransactionMapper::toResponseDto)
                .toList();
    }

    public List<TransactionResponseDto> getUserTransactionsByTypeBetween(UUID id, Category.Type type,
                                                                          LocalDateTime start, LocalDateTime end) {
        return transactionRepository.findByUserAndCategoryTypeAndCreatedAtBetween(findUser(id), type, start, end)
                .stream()
                .map(TransactionMapper::toResponseDto)
                .toList();
    }

    public List<TransactionResponseDto> getUserExpensesBetween(UUID id, LocalDateTime start, LocalDateTime end) {
        return getUserTransactionsByTypeBetween(id, Category.Type.EXPENSE, start, end);
    }

    public List<TransactionResponseDto> getUserIncomesBetween(UUID id, LocalDateTime start, LocalDateTime end) {
        return getUserTransactionsByTypeBetween(id, Category.Type.INCOME, start, end);
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> UserNotFoundException.withId(id));
    }
}
