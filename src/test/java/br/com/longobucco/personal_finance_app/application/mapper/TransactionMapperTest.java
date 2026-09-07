package br.com.longobucco.personal_finance_app.application.mapper;

import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionResponseDto;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.Transaction;
import br.com.longobucco.personal_finance_app.core.domain.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionMapperTest {

    private User validUser() {
        return User.createUser("John Doe", "john.doe@example.com", "11987654321", "secret123",
                new BigDecimal("100.00"));
    }

    private Category validCategory() {
        return Category.createCategory("Salary", Category.Type.INCOME);
    }

    @Test
    void toDomainMapsEachFieldAndAttachesTransactionToUser() {
        User user = validUser();
        Category category = validCategory();
        TransactionRequestDto dto = new TransactionRequestDto("Salary", category.getId(),
                new BigDecimal("50.00"), user.getId(), Transaction.PaymentMethod.PIX);

        Transaction transaction = TransactionMapper.toDomain(dto, user, category);

        assertNotNull(transaction.getId());
        assertEquals("Salary", transaction.getDescription());
        assertEquals(category, transaction.getCategory());
        assertEquals(new BigDecimal("50.00"), transaction.getAmount());
        assertEquals(user, transaction.getUser());
        assertEquals(Transaction.PaymentMethod.PIX, transaction.getPaymentMethod());
        assertTrue(user.getTransactions().contains(transaction));
    }

    @Test
    void toResponseDtoMapsEachDomainFieldIncludingNestedCategoryAndUserId() {
        User user = validUser();
        Category category = validCategory();
        Transaction transaction = Transaction.create("Salary", category, new BigDecimal("50.00"), user,
                Transaction.PaymentMethod.PIX);

        TransactionResponseDto dto = TransactionMapper.toResponseDto(transaction);

        assertEquals(transaction.getId(), dto.id());
        assertEquals(transaction.getDescription(), dto.description());
        assertEquals(category.getId(), dto.category().id());
        assertEquals(category.getName(), dto.category().name());
        assertEquals(category.getType(), dto.category().type());
        assertEquals(transaction.getAmount(), dto.amount());
        assertEquals(user.getId(), dto.userId());
        assertEquals(transaction.getPaymentMethod(), dto.paymentMethod());
        assertEquals(transaction.getCreatedAt(), dto.createdAt());
        assertEquals(transaction.getUpdatedAt(), dto.updatedAt());
    }
}
