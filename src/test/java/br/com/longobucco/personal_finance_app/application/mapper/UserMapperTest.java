package br.com.longobucco.personal_finance_app.application.mapper;

import br.com.longobucco.personal_finance_app.application.dto.user.UserRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.user.UserResponseDto;
import br.com.longobucco.personal_finance_app.core.domain.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserMapperTest {

    @Test
    void toDomainMapsEachFieldToItsMatchingDomainArgument() {
        UserRequestDto dto = new UserRequestDto("John Doe", "john.doe@example.com", "secret123",
                "11987654321", new BigDecimal("100.00"));

        User user = UserMapper.toDomain(dto);

        assertNotNull(user.getId());
        assertEquals("John Doe", user.getName());
        assertEquals("john.doe@example.com", user.getEmail());
        assertEquals("11987654321", user.getPhone());
        assertEquals("secret123", user.getPassword());
        assertEquals(new BigDecimal("100.00"), user.getBalance());
        assertEquals(User.Role.USER, user.getRole());
    }

    @Test
    void toResponseDtoMapsEachDomainFieldAndOmitsPassword() {
        User user = User.createUser("John Doe", "john.doe@example.com", "11987654321", "secret123",
                new BigDecimal("100.00"));

        UserResponseDto dto = UserMapper.toResponseDto(user);

        assertEquals(user.getId(), dto.id());
        assertEquals(user.getName(), dto.name());
        assertEquals(user.getEmail(), dto.email());
        assertEquals(user.getPhone(), dto.phone());
        assertEquals(user.getRole(), dto.role());
        assertEquals(user.getBalance(), dto.balance());
        assertEquals(user.getCreatedAt(), dto.createdAt());
        assertEquals(user.getUpdatedAt(), dto.updatedAt());
    }
}
