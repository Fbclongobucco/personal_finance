package br.com.longobucco.personal_finance_app.application.dto.transaction;

import br.com.longobucco.personal_finance_app.application.dto.category.CategoryResponseDto;
import br.com.longobucco.personal_finance_app.core.domain.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponseDto(UUID id, String description, CategoryResponseDto category, BigDecimal amount,
                                     UUID userId, Transaction.PaymentMethod paymentMethod,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
}
