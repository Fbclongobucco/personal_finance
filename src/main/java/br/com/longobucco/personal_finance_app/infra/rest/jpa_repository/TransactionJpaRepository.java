package br.com.longobucco.personal_finance_app.infra.rest.jpa_repository;

import br.com.longobucco.personal_finance_app.infra.rest.entities.CategoryEntity;
import br.com.longobucco.personal_finance_app.infra.rest.entities.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, UUID> {

    @Override
    @Query("SELECT t FROM TransactionEntity t JOIN FETCH t.category WHERE t.id = :id")
    Optional<TransactionEntity> findById(@Param("id") UUID id);

    @Query("SELECT t FROM TransactionEntity t JOIN FETCH t.category WHERE t.userId = :userId")
    List<TransactionEntity> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT t FROM TransactionEntity t JOIN FETCH t.category "
           + "WHERE t.userId = :userId AND t.createdAt BETWEEN :start AND :end")
    List<TransactionEntity> findByUserIdAndCreatedAtBetween(@Param("userId") UUID userId,
                                                             @Param("start") LocalDateTime start,
                                                             @Param("end") LocalDateTime end);

    @Query("SELECT t FROM TransactionEntity t JOIN FETCH t.category c "
           + "WHERE t.userId = :userId AND c.type = :type AND t.createdAt BETWEEN :start AND :end")
    List<TransactionEntity> findByUserIdAndCategoryTypeAndCreatedAtBetween(@Param("userId") UUID userId,
                                                                            @Param("type") CategoryEntity.Type type,
                                                                            @Param("start") LocalDateTime start,
                                                                            @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionEntity t JOIN t.category c "
           + "WHERE t.userId = :userId AND c.type = :type AND t.paid = false")
    BigDecimal sumPendingExpenses(@Param("userId") UUID userId, @Param("type") CategoryEntity.Type type);

    @Modifying
    @Query("DELETE FROM TransactionEntity t WHERE t.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
