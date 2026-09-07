package br.com.longobucco.personal_finance_app.infra.rest.adapters;

import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.repository.CategoryRepository;
import br.com.longobucco.personal_finance_app.infra.rest.entities.CategoryEntity;
import br.com.longobucco.personal_finance_app.infra.rest.jpa_repository.CategoryJpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final CategoryJpaRepository categoryJpaRepository;

    public CategoryRepositoryAdapter(CategoryJpaRepository categoryJpaRepository) {
        this.categoryJpaRepository = categoryJpaRepository;
    }

    @Override
    public Category save(Category category) {
        CategoryEntity saved = categoryJpaRepository.save(toEntity(category));
        return toDomain(saved);
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return categoryJpaRepository.findById(id).map(CategoryRepositoryAdapter::toDomain);
    }

    @Override
    public void delete(Category category) {
        categoryJpaRepository.deleteById(category.getId());
    }

    @Override
    public List<Category> findAll() {
        return categoryJpaRepository.findAll().stream()
                .map(CategoryRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public List<Category> findByType(Category.Type type) {
        return categoryJpaRepository.findByType(toEntityType(type), Pageable.unpaged())
                .map(CategoryRepositoryAdapter::toDomain)
                .getContent();
    }

    static Category toDomain(CategoryEntity entity) {
        return Category.recover(entity.getId(), entity.getName(), toDomainType(entity.getType()),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    static CategoryEntity toEntity(Category category) {
        return new CategoryEntity(category.getId(), category.getName(), toEntityType(category.getType()),
                category.getCreatedAt(), category.getUpdatedAt());
    }

    static Category.Type toDomainType(CategoryEntity.Type type) {
        return Category.Type.valueOf(type.name());
    }

    static CategoryEntity.Type toEntityType(Category.Type type) {
        return CategoryEntity.Type.valueOf(type.name());
    }
}
