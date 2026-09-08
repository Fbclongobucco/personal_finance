package br.com.longobucco.personal_finance_app.application.mapper;

import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionResponseDto;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionMapperTest {

    private final UUID userId = UUID.randomUUID();

    private Category ownCategory(Category.Type type) {
        return Category.createCategory("Salary", type, userId);
    }

    @Test
    void toDomainMapsEachFieldToItsMatchingDomainArgument() {
        Category category = ownCategory(Category.Type.INCOME);
        TransactionRequestDto dto = new TransactionRequestDto("Salary", category.getId(),
                new BigDecimal("50.00"), userId, Transaction.PaymentMethod.PIX);

        Transaction transaction = TransactionMapper.toDomain(dto, category);

        assertNotNull(transaction.getId());
        assertEquals("Salary", transaction.getDescription());
        assertEquals(category, transaction.getCategory());
        assertEquals(new BigDecimal("50.00"), transaction.getAmount());
        assertEquals(userId, transaction.getUserId());
        assertEquals(Transaction.PaymentMethod.PIX, transaction.getPaymentMethod());
    }

    @Test
    void toDomainLeavesAnExpensePendingWhenTheRequestDoesNotSayOtherwise() {
        Category category = ownCategory(Category.Type.EXPENSE);
        TransactionRequestDto dto = new TransactionRequestDto("Rent", category.getId(),
                new BigDecimal("30.00"), userId, Transaction.PaymentMethod.CASH);

        assertFalse(TransactionMapper.toDomain(dto, category).isPaid());
    }

    @Test
    void toDomainHonoursAnExplicitPaidFlagOnAnExpense() {
        Category category = ownCategory(Category.Type.EXPENSE);
        TransactionRequestDto dto = new TransactionRequestDto("Rent", category.getId(),
                new BigDecimal("30.00"), userId, Transaction.PaymentMethod.CASH, true, null);

        assertTrue(TransactionMapper.toDomain(dto, category).isPaid());
    }

    @Test
    void toResponseDtoMapsEachDomainFieldIncludingNestedCategoryAndUserId() {
        Category category = ownCategory(Category.Type.INCOME);
        Transaction transaction = Transaction.create("Salary", category, new BigDecimal("50.00"), userId,
                Transaction.PaymentMethod.PIX);

        TransactionResponseDto dto = TransactionMapper.toResponseDto(transaction);

        assertEquals(transaction.getId(), dto.id());
        assertEquals(transaction.getDescription(), dto.description());
        assertEquals(category.getId(), dto.category().id());
        assertEquals(category.getName(), dto.category().name());
        assertEquals(category.getType(), dto.category().type());
        assertEquals(transaction.getAmount(), dto.amount());
        assertEquals(userId, dto.userId());
        assertEquals(transaction.getPaymentMethod(), dto.paymentMethod());
        assertEquals(transaction.getCreatedAt(), dto.createdAt());
        assertEquals(transaction.getUpdatedAt(), dto.updatedAt());
    }
}
