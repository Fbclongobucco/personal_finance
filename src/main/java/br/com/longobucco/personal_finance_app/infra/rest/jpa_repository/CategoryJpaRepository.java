package br.com.longobucco.personal_finance_app.infra.rest.jpa_repository;

import br.com.longobucco.personal_finance_app.infra.rest.entities.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, UUID> {

    List<CategoryEntity> findByUserIdInAndNameContainingIgnoreCaseOrderByTypeAscNameAsc(
            Collection<UUID> userIds, String name);

    List<CategoryEntity> findByUserIdInAndTypeAndNameContainingIgnoreCaseOrderByTypeAscNameAsc(
            Collection<UUID> userIds, CategoryEntity.Type type, String name);

    List<CategoryEntity> findByNameContainingIgnoreCaseOrderByTypeAscNameAsc(String name);

    List<CategoryEntity> findByTypeAndNameContainingIgnoreCaseOrderByTypeAscNameAsc(
            CategoryEntity.Type type, String name);

    boolean existsByUserIdAndNameAndType(UUID userId, String name, CategoryEntity.Type type);

    @Modifying
    @Query("DELETE FROM CategoryEntity c WHERE c.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
