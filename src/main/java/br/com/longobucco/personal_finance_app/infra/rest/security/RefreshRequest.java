package br.com.longobucco.personal_finance_app.infra.rest.security;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank String refreshToken) {}
