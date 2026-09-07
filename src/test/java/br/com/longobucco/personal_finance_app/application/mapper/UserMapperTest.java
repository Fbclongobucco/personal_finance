package br.com.longobucco.personal_finance_app.application.mapper;

import br.com.longobucco.personal_finance_app.application.dto.user.UserRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.user.UserResponseDto;
import br.com.longobucco.personal_finance_app.core.domain.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserMapperTest {

    private UserRequestDto validRequestDto() {
        return new UserRequestDto("John Doe", "john.doe@example.com", "secret123", "11987654321",
                new BigDecimal("100.00"));
    }

    @Test
    void toDomainMapsEachFieldToItsMatchingDomainArgument() {
        User user = UserMapper.toDomain(validRequestDto());

        assertNotNull(user.getId());
        assertEquals("John Doe", user.getName());
        assertEquals("john.doe@example.com", user.getEmail());
        assertEquals("11987654321", user.getPhone());
        assertEquals("secret123", user.getPassword());
        assertEquals(User.Role.USER, user.getRole());
        assertEquals(new BigDecimal("100.00"), user.getBalance());
    }

    @Test
    void toResponseDtoExposesBothTheProjectedAndTheSettledBalance() {
        User user = UserMapper.toDomain(validRequestDto());

        UserResponseDto dto = UserMapper.toResponseDto(user, new BigDecimal("30.00"));

        assertEquals(user.getId(), dto.id());
        assertEquals(user.getEmail(), dto.email());
        assertEquals(new BigDecimal("100.00"), dto.balance());
        assertEquals(new BigDecimal("130.00"), dto.settledBalance());
    }
}
