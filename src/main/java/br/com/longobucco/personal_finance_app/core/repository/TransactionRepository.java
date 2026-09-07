package br.com.longobucco.personal_finance_app.core.repository;

import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends Repository<Transaction, UUID> {

    List<Transaction> findByUserId(UUID userId);

    List<Transaction> findByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);

    List<Transaction> findByUserIdAndCategoryTypeAndCreatedAtBetween(UUID userId, Category.Type type,
                                                                      LocalDateTime start, LocalDateTime end);

    BigDecimal sumPendingExpenses(UUID userId);
}
