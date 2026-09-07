package br.com.longobucco.personal_finance_app.application.dto.category;

import br.com.longobucco.personal_finance_app.core.domain.Category;

public record CategoryRequestDto(String name, Category.Type type) {}
