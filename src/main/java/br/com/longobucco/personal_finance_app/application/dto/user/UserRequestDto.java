package br.com.longobucco.personal_finance_app.application.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UserRequestDto(@NotBlank String name,
                             @NotBlank @Email String email,
                             @NotBlank String password,
                             @NotBlank String phone,
                             @NotNull BigDecimal initialBalance) {}
