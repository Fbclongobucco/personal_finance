package br.com.longobucco.personal_finance_app.infra.rest.adapters;

import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.Transaction;
import br.com.longobucco.personal_finance_app.core.domain.User;
import br.com.longobucco.personal_finance_app.core.repository.TransactionRepository;
import br.com.longobucco.personal_finance_app.infra.rest.entities.CategoryEntity;
import br.com.longobucco.personal_finance_app.infra.rest.entities.TransactionEntity;
import br.com.longobucco.personal_finance_app.infra.rest.entities.UserEntity;
import br.com.longobucco.personal_finance_app.infra.rest.jpa_repository.CategoryJpaRepository;
import br.com.longobucco.personal_finance_app.infra.rest.jpa_repository.TransactionJpaRepository;
import br.com.longobucco.personal_finance_app.infra.rest.jpa_repository.UserJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Component
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final TransactionJpaRepository transactionJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final CategoryJpaRepository categoryJpaRepository;

    public TransactionRepositoryAdapter(TransactionJpaRepository transactionJpaRepository,
                                        UserJpaRepository userJpaRepository,
                                        CategoryJpaRepository categoryJpaRepository) {
        this.transactionJpaRepository = transactionJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.categoryJpaRepository = categoryJpaRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity saved = transactionJpaRepository.save(toNewEntity(transaction));
        return toDomain(saved, UserRepositoryAdapter.toDomain(saved.getUser()));
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return transactionJpaRepository.findById(id)
                .map(entity -> toDomain(entity, UserRepositoryAdapter.toDomain(entity.getUser())));
    }

    @Override
    public void delete(Transaction transaction) {
        transactionJpaRepository.deleteById(transaction.getId());
    }

    @Override
    public List<Transaction> findByUser(User user) {
        UserEntity userRef = userJpaRepository.getReferenceById(user.getId());
        Page<TransactionEntity> page = transactionJpaRepository.findByUser(userRef, Pageable.unpaged());
        return toDomainList(page.getContent());
    }

    @Override
    public List<Transaction> findByUserAndCreatedAtBetween(User user, LocalDateTime start, LocalDateTime end) {
        UserEntity userRef = userJpaRepository.getReferenceById(user.getId());
        Page<TransactionEntity> page = transactionJpaRepository.findByUserAndCreatedAtBetween(userRef, start, end,
                Pageable.unpaged());
        return toDomainList(page.getContent());
    }

    @Override
    public List<Transaction> findByUserAndCategoryTypeAndCreatedAtBetween(User user, Category.Type type,
                                                                           LocalDateTime start, LocalDateTime end) {
        UserEntity userRef = userJpaRepository.getReferenceById(user.getId());
        Page<TransactionEntity> page = transactionJpaRepository.findByUserAndCategoryTypeAndCreatedAtBetween(userRef,
                CategoryRepositoryAdapter.toEntityType(type), start, end, Pageable.unpaged());
        return toDomainList(page.getContent());
    }

    private TransactionEntity toNewEntity(Transaction transaction) {
        UserEntity userRef = userJpaRepository.getReferenceById(transaction.getUser().getId());
        CategoryEntity categoryRef = categoryJpaRepository.getReferenceById(transaction.getCategory().getId());
        return new TransactionEntity(transaction.getId(), transaction.getDescription(), categoryRef,
                transaction.getAmount(), userRef, transaction.getCreatedAt(), transaction.getUpdatedAt(),
                toEntityPaymentMethod(transaction.getPaymentMethod()));
    }

    private static List<Transaction> toDomainList(List<TransactionEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        User sharedUser = UserRepositoryAdapter.toDomain(entities.get(0).getUser());
        return entities.stream()
                .map(entity -> toDomain(entity, sharedUser))
                .toList();
    }

    private static Transaction toDomain(TransactionEntity entity, User user) {
        Category category = CategoryRepositoryAdapter.toDomain(entity.getCategory());
        return Transaction.recover(entity.getId(), entity.getDescription(), category, entity.getAmount(), user,
                entity.getCreatedAt(), entity.getUpdatedAt(), toDomainPaymentMethod(entity.getPaymentMethod()));
    }

    private static Transaction.PaymentMethod toDomainPaymentMethod(TransactionEntity.PaymentMethod paymentMethod) {
        return Transaction.PaymentMethod.valueOf(paymentMethod.name());
    }

    private static TransactionEntity.PaymentMethod toEntityPaymentMethod(Transaction.PaymentMethod paymentMethod) {
        return TransactionEntity.PaymentMethod.valueOf(paymentMethod.name());
    }
}
