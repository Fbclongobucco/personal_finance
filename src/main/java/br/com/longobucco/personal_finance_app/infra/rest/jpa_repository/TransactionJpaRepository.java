package br.com.longobucco.personal_finance_app.infra.rest.jpa_repository;

import br.com.longobucco.personal_finance_app.infra.rest.entities.CategoryEntity;
import br.com.longobucco.personal_finance_app.infra.rest.entities.TransactionEntity;
import br.com.longobucco.personal_finance_app.infra.rest.entities.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, UUID> {

    @Override
    @Query("SELECT t FROM TransactionEntity t JOIN FETCH t.category JOIN FETCH t.user WHERE t.id = :id")
    Optional<TransactionEntity> findById(@Param("id") UUID id);

    @Query(value = "SELECT t FROM TransactionEntity t JOIN FETCH t.category JOIN FETCH t.user WHERE t.user = :user",
           countQuery = "SELECT COUNT(t) FROM TransactionEntity t WHERE t.user = :user")
    Page<TransactionEntity> findByUser(@Param("user") UserEntity user, Pageable pageable);

    @Query(value = "SELECT t FROM TransactionEntity t JOIN FETCH t.category JOIN FETCH t.user "
                   + "WHERE t.user = :user AND t.createdAt BETWEEN :start AND :end",
           countQuery = "SELECT COUNT(t) FROM TransactionEntity t "
                   + "WHERE t.user = :user AND t.createdAt BETWEEN :start AND :end")
    Page<TransactionEntity> findByUserAndCreatedAtBetween(@Param("user") UserEntity user,
                                                           @Param("start") LocalDateTime start,
                                                           @Param("end") LocalDateTime end,
                                                           Pageable pageable);

    @Query(value = "SELECT t FROM TransactionEntity t JOIN FETCH t.category c JOIN FETCH t.user "
                   + "WHERE t.user = :user AND c.type = :type AND t.createdAt BETWEEN :start AND :end",
           countQuery = "SELECT COUNT(t) FROM TransactionEntity t JOIN t.category c "
                   + "WHERE t.user = :user AND c.type = :type AND t.createdAt BETWEEN :start AND :end")
    Page<TransactionEntity> findByUserAndCategoryTypeAndCreatedAtBetween(@Param("user") UserEntity user,
                                                                          @Param("type") CategoryEntity.Type type,
                                                                          @Param("start") LocalDateTime start,
                                                                          @Param("end") LocalDateTime end,
                                                                          Pageable pageable);
}
