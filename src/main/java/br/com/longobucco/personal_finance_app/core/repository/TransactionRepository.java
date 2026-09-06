package br.com.longobucco.personal_finance_app.core.repository;

import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.Transaction;
import br.com.longobucco.personal_finance_app.core.domain.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends Repository<Transaction, UUID> {

    List<Transaction> findByUser(User user);

    List<Transaction> findByUserAndCreatedAtBetween(User user, LocalDateTime start, LocalDateTime end);

    List<Transaction> findByUserAndCategoryTypeAndCreatedAtBetween(User user, Category.Type type,
                                                                    LocalDateTime start, LocalDateTime end);
}
