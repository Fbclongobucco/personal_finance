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
import br.com.longobucco.personal_finance_app.core.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class UserUseCase {

    private final UserRepository userRepository;

    public UserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponseDto createUser(UserRequestDto userRequestDto) {
        if (userRepository.existsByEmail(userRequestDto.email())) {
            throw UserAlreadyExistsException.withEmail(userRequestDto.email());
        }
        User user = UserMapper.toDomain(userRequestDto);
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
        return findUser(id).getTransactions().stream()
                .map(TransactionMapper::toResponseDto)
                .toList();
    }

    public List<TransactionResponseDto> getUserTransactionsBetween(UUID id, LocalDateTime start, LocalDateTime end) {
        return findUser(id).getTransactionsBetween(start, end).stream()
                .map(TransactionMapper::toResponseDto)
                .toList();
    }

    public List<TransactionResponseDto> getUserTransactionsByTypeBetween(UUID id, Category.Type type,
                                                                          LocalDateTime start, LocalDateTime end) {
        return findUser(id).getTransactionsByTypeBetween(type, start, end).stream()
                .map(TransactionMapper::toResponseDto)
                .toList();
    }

    public List<TransactionResponseDto> getUserExpensesBetween(UUID id, LocalDateTime start, LocalDateTime end) {
        return findUser(id).getExpensesBetween(start, end).stream()
                .map(TransactionMapper::toResponseDto)
                .toList();
    }

    public List<TransactionResponseDto> getUserIncomesBetween(UUID id, LocalDateTime start, LocalDateTime end) {
        return findUser(id).getIncomesBetween(start, end).stream()
                .map(TransactionMapper::toResponseDto)
                .toList();
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> UserNotFoundException.withId(id));
    }
}
