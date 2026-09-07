package br.com.longobucco.personal_finance_app.application.usecase;

import br.com.longobucco.personal_finance_app.application.dto.category.CategoryRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.category.CategoryResponseDto;
import br.com.longobucco.personal_finance_app.application.dto.category.CategoryUpdateRequestDto;
import br.com.longobucco.personal_finance_app.application.exception.CategoryAlreadyExistsException;
import br.com.longobucco.personal_finance_app.application.exception.CategoryNotFoundException;
import br.com.longobucco.personal_finance_app.application.exception.ForbiddenException;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.User;
import br.com.longobucco.personal_finance_app.core.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    private User owner;
    private User intruder;
    private User admin;

    @BeforeEach
    void setUp() {
        categoryUseCase = new CategoryUseCase(categoryRepository);
        owner = User.createUser("Owner", "owner@example.com", "11987654321", "secret123", BigDecimal.ZERO);
        intruder = User.createUser("Intruder", "intruder@example.com", "11987654321", "secret123", BigDecimal.ZERO);
        admin = User.createAdmin("Admin", "admin@example.com", "11987654321", "secret123", BigDecimal.ZERO,
                LocalDate.now(), LocalDate.now());
    }

    private Category ownersCategory() {
        return Category.createCategory("Salary", Category.Type.INCOME, owner.getId());
    }

    @Test
    void createCategorySavesAndReturnsCategory() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponseDto result = categoryUseCase.createCategory(
                new CategoryRequestDto("Salary", Category.Type.INCOME), owner.getId());

        assertEquals("Salary", result.name());
        assertEquals(Category.Type.INCOME, result.type());
        assertEquals(owner.getId(), result.userId());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategoryThrowsWhenTheOwnerAlreadyHasOneWithThatNameAndType() {
        when(categoryRepository.existsByOwnerIdAndNameAndType(owner.getId(), "Salary", Category.Type.INCOME))
                .thenReturn(true);

        assertThrows(CategoryAlreadyExistsException.class, () -> categoryUseCase.createCategory(
                new CategoryRequestDto("Salary", Category.Type.INCOME), owner.getId()));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void createCategoryAllowsTheSameNameForADifferentType() {
        when(categoryRepository.existsByOwnerIdAndNameAndType(owner.getId(), "Bonus", Category.Type.EXPENSE))
                .thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponseDto result = categoryUseCase.createCategory(
                new CategoryRequestDto("Bonus", Category.Type.EXPENSE), owner.getId());

        assertEquals(Category.Type.EXPENSE, result.type());
    }

    @Test
    void getCategoryByIdReturnsCategoryForItsOwner() {
        Category category = ownersCategory();
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        CategoryResponseDto result = categoryUseCase.getCategoryById(category.getId(), owner);

        assertEquals(category.getId(), result.id());
    }

    @Test
    void getCategoryByIdReturnsCategoryForAnAdmin() {
        Category category = ownersCategory();
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        CategoryResponseDto result = categoryUseCase.getCategoryById(category.getId(), admin);

        assertEquals(category.getId(), result.id());
    }

    @Test
    void getCategoryByIdHidesAnotherUsersCategoryAsNotFound() {
        Category category = ownersCategory();
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        assertThrows(CategoryNotFoundException.class,
                () -> categoryUseCase.getCategoryById(category.getId(), intruder));
    }

    @Test
    void getCategoryByIdThrowsCategoryNotFoundExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryUseCase.getCategoryById(id, owner));
    }

    @Test
    void searchCategoriesReturnsTheOwnersMappedCategories() {
        Category income = Category.createCategory("Salary", Category.Type.INCOME, owner.getId());
        Category expense = Category.createCategory("Rent", Category.Type.EXPENSE, owner.getId());
        when(categoryRepository.search(List.of(owner.getId()), null, null)).thenReturn(List.of(income, expense));

        List<CategoryResponseDto> result = categoryUseCase.searchCategories(List.of(owner.getId()), null, null, owner);

        assertEquals(2, result.size());
    }

    @Test
    void searchCategoriesFallsBackToTheCallersOwnCategoriesWhenNoOwnerIsGiven() {
        Category income = Category.createCategory("Salary", Category.Type.INCOME, owner.getId());
        when(categoryRepository.search(List.of(owner.getId()), null, null)).thenReturn(List.of(income));

        assertEquals(1, categoryUseCase.searchCategories(null, null, null, owner).size());
        assertEquals(1, categoryUseCase.searchCategories(List.of(), null, null, owner).size());
    }

    @Test
    void searchCategoriesPassesTheTypeAndNameFiltersThrough() {
        Category income = Category.createCategory("Salary", Category.Type.INCOME, owner.getId());
        when(categoryRepository.search(List.of(owner.getId()), Category.Type.INCOME, "sal"))
                .thenReturn(List.of(income));

        List<CategoryResponseDto> result = categoryUseCase.searchCategories(List.of(owner.getId()),
                Category.Type.INCOME, "sal", owner);

        assertEquals(1, result.size());
        assertEquals(income.getId(), result.get(0).id());
    }

    @Test
    void searchCategoriesIsForbiddenForAnotherRegularUser() {
        assertThrows(ForbiddenException.class,
                () -> categoryUseCase.searchCategories(List.of(owner.getId()), null, null, intruder));
        verify(categoryRepository, never()).search(any(), any(), any());
    }

    @Test
    void searchCategoriesLetsAnAdminAskForSeveralUsersAtOnce() {
        Category ownersCategory = Category.createCategory("Salary", Category.Type.INCOME, owner.getId());
        Category intrudersCategory = Category.createCategory("Rent", Category.Type.EXPENSE, intruder.getId());
        List<UUID> owners = List.of(owner.getId(), intruder.getId());
        when(categoryRepository.search(owners, null, null))
                .thenReturn(List.of(ownersCategory, intrudersCategory));

        List<CategoryResponseDto> result = categoryUseCase.searchCategories(owners, null, null, admin);

        assertEquals(2, result.size());
    }

    @Test
    void searchCategoriesRejectsAMixedListWhenTheCallerIsNotAnAdmin() {
        assertThrows(ForbiddenException.class, () -> categoryUseCase.searchCategories(
                List.of(owner.getId(), intruder.getId()), null, null, owner));
        verify(categoryRepository, never()).search(any(), any(), any());
    }

    @Test
    void searchEveryUsersCategoriesReturnsThemForAnAdmin() {
        Category ownersCategory = Category.createCategory("Salary", Category.Type.INCOME, owner.getId());
        when(categoryRepository.searchEveryOwner(null, null)).thenReturn(List.of(ownersCategory));

        List<CategoryResponseDto> result = categoryUseCase.searchEveryUsersCategories(null, null, admin);

        assertEquals(1, result.size());
    }

    @Test
    void searchEveryUsersCategoriesIsForbiddenForARegularUser() {
        assertThrows(ForbiddenException.class,
                () -> categoryUseCase.searchEveryUsersCategories(null, null, owner));
        verify(categoryRepository, never()).searchEveryOwner(any(), any());
    }

    @Test
    void renameCategoryUpdatesTheNameForItsOwner() {
        Category category = ownersCategory();
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(categoryRepository.existsByOwnerIdAndNameAndType(owner.getId(), "Salário", Category.Type.INCOME))
                .thenReturn(false);
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryResponseDto result = categoryUseCase.renameCategory(category.getId(),
                new CategoryUpdateRequestDto("Salário"), owner);

        assertEquals("Salário", result.name());
        verify(categoryRepository).save(category);
    }

    @Test
    void renameCategoryThrowsWhenTheNewNameIsAlreadyTaken() {
        Category category = ownersCategory();
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(categoryRepository.existsByOwnerIdAndNameAndType(owner.getId(), "Bonus", Category.Type.INCOME))
                .thenReturn(true);

        assertThrows(CategoryAlreadyExistsException.class, () -> categoryUseCase.renameCategory(category.getId(),
                new CategoryUpdateRequestDto("Bonus"), owner));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void renameCategoryIsForbiddenForAnAdminWhoIsNotTheOwner() {
        Category category = ownersCategory();
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        assertThrows(ForbiddenException.class, () -> categoryUseCase.renameCategory(category.getId(),
                new CategoryUpdateRequestDto("Bonus"), admin));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void deleteCategoryDeletesCategoryForItsOwner() {
        Category category = ownersCategory();
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        categoryUseCase.deleteCategory(category.getId(), owner);

        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteCategoryIsForbiddenForAnAdminWhoIsNotTheOwner() {
        Category category = ownersCategory();
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        assertThrows(ForbiddenException.class, () -> categoryUseCase.deleteCategory(category.getId(), admin));
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void deleteCategoryHidesAnotherUsersCategoryAsNotFound() {
        Category category = ownersCategory();
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        assertThrows(CategoryNotFoundException.class,
                () -> categoryUseCase.deleteCategory(category.getId(), intruder));
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void deleteCategoryThrowsCategoryNotFoundExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryUseCase.deleteCategory(id, owner));
        verify(categoryRepository, never()).delete(any(Category.class));
    }
}
