package br.com.longobucco.personal_finance_app.application.dto.category;

import jakarta.validation.constraints.NotBlank;

public record CategoryUpdateRequestDto(@NotBlank String name) {}
