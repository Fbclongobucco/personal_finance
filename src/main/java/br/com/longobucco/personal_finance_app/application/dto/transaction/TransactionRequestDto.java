package br.com.longobucco.personal_finance_app.application.dto.transaction;

import br.com.longobucco.personal_finance_app.core.domain.Transaction;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionRequestDto(String description, UUID categoryId, BigDecimal amount, UUID userId,
                                    Transaction.PaymentMethod paymentMethod) {
}
