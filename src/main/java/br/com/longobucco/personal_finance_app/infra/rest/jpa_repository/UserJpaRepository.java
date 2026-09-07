package br.com.longobucco.personal_finance_app.infra.rest.jpa_repository;

import br.com.longobucco.personal_finance_app.infra.rest.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
