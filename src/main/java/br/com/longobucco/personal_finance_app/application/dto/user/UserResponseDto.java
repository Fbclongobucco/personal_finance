package br.com.longobucco.personal_finance_app.application.dto.user;

import br.com.longobucco.personal_finance_app.core.domain.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UserResponseDto(UUID id, String name, String email, String phone,
                              User.Role role, BigDecimal balance, LocalDate createdAt, LocalDate updatedAt) {
}
