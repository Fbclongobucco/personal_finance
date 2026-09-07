package br.com.longobucco.personal_finance_app.application.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDto(@NotBlank String refreshToken) {}
