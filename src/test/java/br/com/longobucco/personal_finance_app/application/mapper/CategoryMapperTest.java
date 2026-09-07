package br.com.longobucco.personal_finance_app.application.mapper;

import br.com.longobucco.personal_finance_app.application.dto.category.CategoryRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.category.CategoryResponseDto;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CategoryMapperTest {

    @Test
    void toDomainMapsEachFieldToItsMatchingDomainArgument() {
        CategoryRequestDto dto = new CategoryRequestDto("Salary", Category.Type.INCOME);
        UUID ownerId = UUID.randomUUID();

        Category category = CategoryMapper.toDomain(dto, ownerId);

        assertNotNull(category.getId());
        assertEquals("Salary", category.getName());
        assertEquals(Category.Type.INCOME, category.getType());
        assertEquals(ownerId, category.getOwnerId());
    }

    @Test
    void toResponseDtoMapsEachDomainField() {
        Category category = Category.createCategory("Rent", Category.Type.EXPENSE, UUID.randomUUID());

        CategoryResponseDto dto = CategoryMapper.toResponseDto(category);

        assertEquals(category.getId(), dto.id());
        assertEquals(category.getName(), dto.name());
        assertEquals(category.getType(), dto.type());
        assertEquals(category.getOwnerId(), dto.userId());
        assertEquals(category.getCreatedAt(), dto.createdAt());
        assertEquals(category.getUpdatedAt(), dto.updatedAt());
    }
}
