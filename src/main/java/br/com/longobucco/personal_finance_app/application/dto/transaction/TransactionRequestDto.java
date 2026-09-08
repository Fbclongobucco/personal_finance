package br.com.longobucco.personal_finance_app.application.dto.transaction;

import br.com.longobucco.personal_finance_app.core.domain.Transaction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionRequestDto(@NotBlank String description,
                                    @NotNull UUID categoryId,
                                    @NotNull @Positive BigDecimal amount,
                                    @NotNull UUID userId,
                                    @NotNull Transaction.PaymentMethod paymentMethod,
                                    Boolean paid,
                                    LocalDateTime date) {
    public TransactionRequestDto(String description, UUID categoryId, BigDecimal amount, UUID userId,
                                 Transaction.PaymentMethod paymentMethod) {
        this(description, categoryId, amount, userId, paymentMethod, null, null);
    }
}
