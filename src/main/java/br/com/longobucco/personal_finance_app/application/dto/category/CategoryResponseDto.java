package br.com.longobucco.personal_finance_app.application.dto.category;

import br.com.longobucco.personal_finance_app.core.domain.Category;

import java.time.LocalDateTime;
import java.util.UUID;

public record CategoryResponseDto(UUID id, String name, Category.Type type,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
}
