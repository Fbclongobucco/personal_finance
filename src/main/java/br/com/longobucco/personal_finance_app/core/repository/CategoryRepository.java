package br.com.longobucco.personal_finance_app.core.repository;

import br.com.longobucco.personal_finance_app.core.domain.Category;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends Repository<Category, UUID> {

    List<Category> search(Collection<UUID> ownerIds, Category.Type type, String nameContains);

    List<Category> searchEveryOwner(Category.Type type, String nameContains);

    boolean existsByOwnerIdAndNameAndType(UUID ownerId, String name, Category.Type type);
}
