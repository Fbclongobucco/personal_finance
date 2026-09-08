package br.com.longobucco.personal_finance_app.application.mapper;

import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionResponseDto;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.Transaction;

public final class TransactionMapper {

    private TransactionMapper() {
    }

    public static Transaction toDomain(TransactionRequestDto dto, Category category) {
        if (dto.paid() == null) {
            return Transaction.create(dto.description(), category, dto.amount(), dto.userId(), dto.paymentMethod(),
                    dto.date());
        }
        return Transaction.create(dto.description(), category, dto.amount(), dto.userId(), dto.paymentMethod(),
                dto.paid(), dto.date());
    }

    public static TransactionResponseDto toResponseDto(Transaction transaction) {
        return new TransactionResponseDto(transaction.getId(), transaction.getDescription(),
                CategoryMapper.toResponseDto(transaction.getCategory()), transaction.getAmount(),
                transaction.getUserId(), transaction.getPaymentMethod(),
                transaction.getCreatedAt(), transaction.getUpdatedAt(), transaction.isPaid());
    }
}
