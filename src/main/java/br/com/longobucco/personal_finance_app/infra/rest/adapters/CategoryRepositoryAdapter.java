package br.com.longobucco.personal_finance_app.infra.rest.adapters;

import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.repository.CategoryRepository;
import br.com.longobucco.personal_finance_app.infra.rest.entities.CategoryEntity;
import br.com.longobucco.personal_finance_app.infra.rest.jpa_repository.CategoryJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
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
    public List<Category> search(Collection<UUID> ownerIds, Category.Type type, String nameContains) {
        String name = normalise(nameContains);
        List<CategoryEntity> found = type == null
                ? categoryJpaRepository.findByUserIdInAndNameContainingIgnoreCaseOrderByTypeAscNameAsc(ownerIds, name)
                : categoryJpaRepository.findByUserIdInAndTypeAndNameContainingIgnoreCaseOrderByTypeAscNameAsc(
                        ownerIds, toEntityType(type), name);
        return toDomainList(found);
    }

    @Override
    public List<Category> searchEveryOwner(Category.Type type, String nameContains) {
        String name = normalise(nameContains);
        List<CategoryEntity> found = type == null
                ? categoryJpaRepository.findByNameContainingIgnoreCaseOrderByTypeAscNameAsc(name)
                : categoryJpaRepository.findByTypeAndNameContainingIgnoreCaseOrderByTypeAscNameAsc(
                        toEntityType(type), name);
        return toDomainList(found);
    }

    private static String normalise(String nameContains) {
        return nameContains == null ? "" : nameContains.trim();
    }

    @Override
    public boolean existsByOwnerIdAndNameAndType(UUID ownerId, String name, Category.Type type) {
        return categoryJpaRepository.existsByUserIdAndNameAndType(ownerId, name, toEntityType(type));
    }

    private static List<Category> toDomainList(List<CategoryEntity> entities) {
        return entities.stream()
                .map(CategoryRepositoryAdapter::toDomain)
                .toList();
    }

    private static CategoryEntity toEntity(Category category) {
        return new CategoryEntity(category.getId(), category.getName(), toEntityType(category.getType()),
                category.getOwnerId(), category.getCreatedAt(), category.getUpdatedAt());
    }

    static Category toDomain(CategoryEntity entity) {
        return Category.recover(entity.getId(), entity.getName(), toDomainType(entity.getType()),
                entity.getUserId(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    static Category.Type toDomainType(CategoryEntity.Type type) {
        return Category.Type.valueOf(type.name());
    }

    static CategoryEntity.Type toEntityType(Category.Type type) {
        return CategoryEntity.Type.valueOf(type.name());
    }
}
