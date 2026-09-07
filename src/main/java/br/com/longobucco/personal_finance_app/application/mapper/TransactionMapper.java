package br.com.longobucco.personal_finance_app.application.mapper;

import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionResponseDto;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.Transaction;
import br.com.longobucco.personal_finance_app.core.domain.User;

public final class TransactionMapper {

    private TransactionMapper() {
    }

    public static Transaction toDomain(TransactionRequestDto dto, User user, Category category) {
        if (dto.paid() == null) {
            return Transaction.create(dto.description(), category, dto.amount(), user, dto.paymentMethod());
        }
        return Transaction.create(dto.description(), category, dto.amount(), user, dto.paymentMethod(), dto.paid());
    }

    public static TransactionResponseDto toResponseDto(Transaction transaction) {
        return new TransactionResponseDto(transaction.getId(), transaction.getDescription(),
                CategoryMapper.toResponseDto(transaction.getCategory()), transaction.getAmount(),
                transaction.getUser().getId(), transaction.getPaymentMethod(),
                transaction.getCreatedAt(), transaction.getUpdatedAt(), transaction.isPaid());
    }
}
