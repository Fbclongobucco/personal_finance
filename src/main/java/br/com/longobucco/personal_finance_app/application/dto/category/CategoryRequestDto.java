package br.com.longobucco.personal_finance_app.application.dto.category;

import br.com.longobucco.personal_finance_app.core.domain.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRequestDto(@NotBlank String name, @NotNull Category.Type type) {}
