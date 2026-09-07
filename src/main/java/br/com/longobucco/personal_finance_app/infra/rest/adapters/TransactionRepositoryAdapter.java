package br.com.longobucco.personal_finance_app.infra.rest.adapters;

import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.Transaction;
import br.com.longobucco.personal_finance_app.core.repository.TransactionRepository;
import br.com.longobucco.personal_finance_app.infra.rest.entities.CategoryEntity;
import br.com.longobucco.personal_finance_app.infra.rest.entities.TransactionEntity;
import br.com.longobucco.personal_finance_app.infra.rest.jpa_repository.CategoryJpaRepository;
import br.com.longobucco.personal_finance_app.infra.rest.jpa_repository.TransactionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final TransactionJpaRepository transactionJpaRepository;
    private final CategoryJpaRepository categoryJpaRepository;

    public TransactionRepositoryAdapter(TransactionJpaRepository transactionJpaRepository,
                                        CategoryJpaRepository categoryJpaRepository) {
        this.transactionJpaRepository = transactionJpaRepository;
        this.categoryJpaRepository = categoryJpaRepository;
    }

    @Override
    @Transactional
    public Transaction save(Transaction transaction) {
        transactionJpaRepository.save(toNewEntity(transaction));
        return transaction;
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return transactionJpaRepository.findById(id).map(TransactionRepositoryAdapter::toDomain);
    }

    @Override
    public void delete(Transaction transaction) {
        transactionJpaRepository.deleteById(transaction.getId());
    }

    @Override
    public List<Transaction> findByUserId(UUID userId) {
        return toDomainList(transactionJpaRepository.findByUserId(userId));
    }

    @Override
    public List<Transaction> findByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end) {
        return toDomainList(transactionJpaRepository.findByUserIdAndCreatedAtBetween(userId, start, end));
    }

    @Override
    public List<Transaction> findByUserIdAndCategoryTypeAndCreatedAtBetween(UUID userId, Category.Type type,
                                                                             LocalDateTime start, LocalDateTime end) {
        return toDomainList(transactionJpaRepository.findByUserIdAndCategoryTypeAndCreatedAtBetween(userId,
                CategoryRepositoryAdapter.toEntityType(type), start, end));
    }

    @Override
    public BigDecimal sumPendingExpenses(UUID userId) {
        return transactionJpaRepository.sumPendingExpenses(userId, CategoryEntity.Type.EXPENSE);
    }

    private TransactionEntity toNewEntity(Transaction transaction) {
        CategoryEntity categoryRef = categoryJpaRepository.getReferenceById(transaction.getCategory().getId());
        return new TransactionEntity(transaction.getId(), transaction.getDescription(), categoryRef,
                transaction.getAmount(), transaction.getUserId(), transaction.getCreatedAt(),
                transaction.getUpdatedAt(), toEntityPaymentMethod(transaction.getPaymentMethod()),
                transaction.isPaid());
    }

    private static List<Transaction> toDomainList(List<TransactionEntity> entities) {
        return entities.stream()
                .map(TransactionRepositoryAdapter::toDomain)
                .toList();
    }

    private static Transaction toDomain(TransactionEntity entity) {
        Category category = CategoryRepositoryAdapter.toDomain(entity.getCategory());
        return Transaction.recover(entity.getId(), entity.getDescription(), category, entity.getAmount(),
                entity.getUserId(), entity.getCreatedAt(), entity.getUpdatedAt(),
                toDomainPaymentMethod(entity.getPaymentMethod()), entity.isPaid());
    }

    private static Transaction.PaymentMethod toDomainPaymentMethod(TransactionEntity.PaymentMethod paymentMethod) {
        return Transaction.PaymentMethod.valueOf(paymentMethod.name());
    }

    private static TransactionEntity.PaymentMethod toEntityPaymentMethod(Transaction.PaymentMethod paymentMethod) {
        return TransactionEntity.PaymentMethod.valueOf(paymentMethod.name());
    }
}
