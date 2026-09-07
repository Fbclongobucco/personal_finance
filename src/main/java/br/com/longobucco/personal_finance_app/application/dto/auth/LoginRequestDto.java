package br.com.longobucco.personal_finance_app.application.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(@NotBlank @Email String email, @NotBlank String password) {}
