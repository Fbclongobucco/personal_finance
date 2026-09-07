package br.com.longobucco.personal_finance_app.application.usecase;

import br.com.longobucco.personal_finance_app.application.dto.category.CategoryRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.category.CategoryResponseDto;
import br.com.longobucco.personal_finance_app.application.exception.CategoryNotFoundException;
import br.com.longobucco.personal_finance_app.application.mapper.CategoryMapper;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.repository.CategoryRepository;

import java.util.List;
import java.util.UUID;

public class CategoryUseCase {

    private final CategoryRepository categoryRepository;

    public CategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryResponseDto createCategory(CategoryRequestDto categoryRequestDto) {
        Category category = CategoryMapper.toDomain(categoryRequestDto);
        Category savedCategory = categoryRepository.save(category);
        return CategoryMapper.toResponseDto(savedCategory);
    }

    public CategoryResponseDto getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> CategoryNotFoundException.withId(id));
        return CategoryMapper.toResponseDto(category);
    }

    public List<CategoryResponseDto> listCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryMapper::toResponseDto)
                .toList();
    }

    public List<CategoryResponseDto> listCategoriesByType(Category.Type type) {
        return categoryRepository.findByType(type).stream()
                .map(CategoryMapper::toResponseDto)
                .toList();
    }

    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> CategoryNotFoundException.withId(id));
        categoryRepository.delete(category);
    }
}
