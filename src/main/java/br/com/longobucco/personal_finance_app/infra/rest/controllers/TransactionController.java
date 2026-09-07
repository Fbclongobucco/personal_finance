package br.com.longobucco.personal_finance_app.infra.rest.controllers;

import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionResponseDto;
import br.com.longobucco.personal_finance_app.application.usecase.TransactionUseCase;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.User;
import br.com.longobucco.personal_finance_app.infra.rest.security.AccessGuard;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Tag(name = "Transactions", description = "Income and expense transactions")
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionUseCase transactionUseCase;

    public TransactionController(TransactionUseCase transactionUseCase) {
        this.transactionUseCase = transactionUseCase;
    }

    @Operation(summary = "Create a transaction",
            description = "Registers the transaction and applies it to the user's balance. Only the transaction's "
                    + "own user may create it — not even an ADMIN can create a transaction on someone else's behalf.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transaction created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Caller is not the transaction's own user"),
            @ApiResponse(responseCode = "404", description = "User or category not found")
    })
    @PostMapping
    public ResponseEntity<TransactionResponseDto> create(@Valid @RequestBody TransactionRequestDto request,
                                                          @AuthenticationPrincipal User currentUser) {
        AccessGuard.requireOwner(currentUser, request.userId());
        TransactionResponseDto created = transactionUseCase.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Get a transaction by id", description = "Callers may only fetch their own transactions, unless they are an ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction found"),
            @ApiResponse(responseCode = "403", description = "Caller is not the owner and not an ADMIN"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponseDto> getById(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        TransactionResponseDto transaction = transactionUseCase.getTransactionById(id);
        AccessGuard.requireOwnerOrAdmin(currentUser, transaction.userId());
        return ResponseEntity.ok(transaction);
    }

    @Operation(summary = "List a user's transactions",
            description = "Without start/end, returns all of the user's transactions. With start/end, "
                    + "optionally filtered by category type (INCOME or EXPENSE). Callers may only list their own "
                    + "transactions, unless they are an ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions found"),
            @ApiResponse(responseCode = "403", description = "Caller is not the owner and not an ADMIN"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping
    public ResponseEntity<List<TransactionResponseDto>> list(
            @Parameter(description = "Id of the user whose transactions are listed", required = true)
            @RequestParam UUID userId,
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "Period start (inclusive), required together with end")
            @RequestParam(required = false) LocalDateTime start,
            @Parameter(description = "Period end (inclusive), required together with start")
            @RequestParam(required = false) LocalDateTime end,
            @Parameter(description = "Optional category type filter, only applied when start/end are set")
            @RequestParam(required = false) Category.Type type) {
        AccessGuard.requireOwnerOrAdmin(currentUser, userId);
        if (start == null || end == null) {
            return ResponseEntity.ok(transactionUseCase.listTransactionsByUser(userId));
        }
        if (type != null) {
            return ResponseEntity.ok(transactionUseCase.listTransactionsByUserAndTypeBetween(userId, type, start, end));
        }
        return ResponseEntity.ok(transactionUseCase.listTransactionsByUserBetween(userId, start, end));
    }

    @Operation(summary = "Delete a transaction by id",
            description = "Only the transaction's own user may delete it — not even an ADMIN can delete someone "
                    + "else's transaction.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Transaction deleted"),
            @ApiResponse(responseCode = "403", description = "Caller is not the transaction's own user"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        TransactionResponseDto transaction = transactionUseCase.getTransactionById(id);
        AccessGuard.requireOwner(currentUser, transaction.userId());
        transactionUseCase.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Settle an expense transaction (\"dar baixa\")",
            description = "Marks an EXPENSE transaction as paid. Only the transaction's own user may settle it — "
                    + "not even an ADMIN can settle someone else's expense.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction settled"),
            @ApiResponse(responseCode = "400", description = "Transaction is not an EXPENSE"),
            @ApiResponse(responseCode = "403", description = "Caller is not the transaction's own user"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @PatchMapping("/{id}/settle")
    public ResponseEntity<TransactionResponseDto> settle(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        TransactionResponseDto transaction = transactionUseCase.getTransactionById(id);
        AccessGuard.requireOwner(currentUser, transaction.userId());
        return ResponseEntity.ok(transactionUseCase.settleTransaction(id));
    }
}
