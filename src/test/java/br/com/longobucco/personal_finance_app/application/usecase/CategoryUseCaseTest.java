package br.com.longobucco.personal_finance_app.application.usecase;

import br.com.longobucco.personal_finance_app.application.dto.category.CategoryRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.category.CategoryResponseDto;
import br.com.longobucco.personal_finance_app.application.exception.CategoryNotFoundException;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryUseCaseTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryUseCase categoryUseCase;

    @BeforeEach
    void setUp() {
        categoryUseCase = new CategoryUseCase(categoryRepository);
    }

    private Category validCategory() {
        return Category.createCategory("Salary", Category.Type.INCOME);
    }

    @Test
    void createCategorySavesAndReturnsCategory() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponseDto result = categoryUseCase.createCategory(new CategoryRequestDto("Salary", Category.Type.INCOME));

        assertEquals("Salary", result.name());
        assertEquals(Category.Type.INCOME, result.type());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void getCategoryByIdReturnsCategoryWhenFound() {
        Category category = validCategory();
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        CategoryResponseDto result = categoryUseCase.getCategoryById(category.getId());

        assertEquals(category.getId(), result.id());
    }

    @Test
    void getCategoryByIdThrowsCategoryNotFoundExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryUseCase.getCategoryById(id));
    }

    @Test
    void listCategoriesReturnsAllMappedCategories() {
        Category income = Category.createCategory("Salary", Category.Type.INCOME);
        Category expense = Category.createCategory("Rent", Category.Type.EXPENSE);
        when(categoryRepository.findAll()).thenReturn(List.of(income, expense));

        List<CategoryResponseDto> result = categoryUseCase.listCategories();

        assertEquals(2, result.size());
    }

    @Test
    void listCategoriesByTypeReturnsOnlyMatchingCategories() {
        Category income = Category.createCategory("Salary", Category.Type.INCOME);
        when(categoryRepository.findByType(Category.Type.INCOME)).thenReturn(List.of(income));

        List<CategoryResponseDto> result = categoryUseCase.listCategoriesByType(Category.Type.INCOME);

        assertEquals(1, result.size());
        assertEquals(income.getId(), result.get(0).id());
    }

    @Test
    void deleteCategoryDeletesCategoryWhenFound() {
        Category category = validCategory();
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        categoryUseCase.deleteCategory(category.getId());

        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategoryThrowsCategoryNotFoundExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryUseCase.deleteCategory(id));
        verify(categoryRepository, never()).delete(any(Category.class));
    }
}
