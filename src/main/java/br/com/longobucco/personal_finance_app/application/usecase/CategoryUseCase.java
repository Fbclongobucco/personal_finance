package br.com.longobucco.personal_finance_app.application.usecase;

import br.com.longobucco.personal_finance_app.application.dto.category.CategoryRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.category.CategoryResponseDto;
import br.com.longobucco.personal_finance_app.application.dto.category.CategoryUpdateRequestDto;
import br.com.longobucco.personal_finance_app.application.exception.CategoryAlreadyExistsException;
import br.com.longobucco.personal_finance_app.application.exception.CategoryNotFoundException;
import br.com.longobucco.personal_finance_app.application.mapper.CategoryMapper;
import br.com.longobucco.personal_finance_app.application.security.AccessGuard;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.User;
import br.com.longobucco.personal_finance_app.core.repository.CategoryRepository;

import java.util.List;
import java.util.UUID;

public class CategoryUseCase {

    private final CategoryRepository categoryRepository;

    public CategoryUseCase(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryResponseDto createCategory(CategoryRequestDto categoryRequestDto, UUID ownerId) {
        Category category = CategoryMapper.toDomain(categoryRequestDto, ownerId);
        requireNameAvailable(ownerId, category.getName(), category.getType());
        Category savedCategory = categoryRepository.save(category);
        return CategoryMapper.toResponseDto(savedCategory);
    }

    public CategoryResponseDto getCategoryById(UUID id, User requester) {
        return CategoryMapper.toResponseDto(findVisibleCategory(id, requester));
    }

    public List<CategoryResponseDto> searchCategories(List<UUID> ownerIds, Category.Type type, String nameContains,
                                                       User requester) {
        List<UUID> owners = ownerIds == null || ownerIds.isEmpty() ? List.of(requester.getId()) : ownerIds;
        owners.forEach(ownerId -> AccessGuard.requireOwnerOrAdmin(requester, ownerId));
        return toResponseDtos(categoryRepository.search(owners, type, nameContains));
    }

    public List<CategoryResponseDto> searchEveryUsersCategories(Category.Type type, String nameContains,
                                                                 User requester) {
        AccessGuard.requireAdmin(requester);
        return toResponseDtos(categoryRepository.searchEveryOwner(type, nameContains));
    }

    public CategoryResponseDto renameCategory(UUID id, CategoryUpdateRequestDto request, User requester) {
        Category category = findOwnCategory(id, requester);
        if (!request.name().equals(category.getName())) {
            requireNameAvailable(category.getOwnerId(), request.name(), category.getType());
        }
        category.rename(request.name());
        return CategoryMapper.toResponseDto(categoryRepository.save(category));
    }

    public void deleteCategory(UUID id, User requester) {
        categoryRepository.delete(findOwnCategory(id, requester));
    }

    private static List<CategoryResponseDto> toResponseDtos(List<Category> categories) {
        return categories.stream()
                .map(CategoryMapper::toResponseDto)
                .toList();
    }

    private Category findVisibleCategory(UUID id, User requester) {
        return categoryRepository.findById(id)
                .filter(category -> AccessGuard.isOwnerOrAdmin(requester, category.getOwnerId()))
                .orElseThrow(() -> CategoryNotFoundException.withId(id));
    }

    private Category findOwnCategory(UUID id, User requester) {
        Category category = findVisibleCategory(id, requester);
        AccessGuard.requireOwner(requester, category.getOwnerId());
        return category;
    }

    private void requireNameAvailable(UUID ownerId, String name, Category.Type type) {
        if (categoryRepository.existsByOwnerIdAndNameAndType(ownerId, name, type)) {
            throw CategoryAlreadyExistsException.withNameAndType(name, type);
        }
    }
}
