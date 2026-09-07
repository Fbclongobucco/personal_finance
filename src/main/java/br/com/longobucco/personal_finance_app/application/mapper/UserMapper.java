package br.com.longobucco.personal_finance_app.application.mapper;

import br.com.longobucco.personal_finance_app.application.dto.user.UserRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.user.UserResponseDto;
import br.com.longobucco.personal_finance_app.core.domain.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static User toDomain(UserRequestDto dto) {
        return User.createUser(dto.name(), dto.email(), dto.phone(), dto.password(), dto.initialBalance());
    }

    public static UserResponseDto toResponseDto(User user) {
        return new UserResponseDto(user.getId(), user.getName(), user.getEmail(), user.getPhone(),
                user.getRole(), user.getBalance(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
