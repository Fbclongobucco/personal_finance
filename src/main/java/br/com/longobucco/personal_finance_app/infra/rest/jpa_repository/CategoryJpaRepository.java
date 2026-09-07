package br.com.longobucco.personal_finance_app.infra.rest.jpa_repository;

import br.com.longobucco.personal_finance_app.infra.rest.entities.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, UUID> {

    Page<CategoryEntity> findByType(CategoryEntity.Type type, Pageable pageable);
}
