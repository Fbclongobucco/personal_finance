package br.com.longobucco.personal_finance_app.application.mapper;

import br.com.longobucco.personal_finance_app.application.dto.category.CategoryRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.category.CategoryResponseDto;
import br.com.longobucco.personal_finance_app.core.domain.Category;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static Category toDomain(CategoryRequestDto dto) {
        return Category.createCategory(dto.name(), dto.type());
    }

    public static CategoryResponseDto toResponseDto(Category category) {
        return new CategoryResponseDto(category.getId(), category.getName(), category.getType(),
                category.getCreatedAt(), category.getUpdatedAt());
    }
}
