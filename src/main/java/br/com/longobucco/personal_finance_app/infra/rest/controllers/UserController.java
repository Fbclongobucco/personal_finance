package br.com.longobucco.personal_finance_app.infra.rest.controllers;

import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionResponseDto;
import br.com.longobucco.personal_finance_app.application.dto.user.UserRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.user.UserResponseDto;
import br.com.longobucco.personal_finance_app.application.usecase.UserUseCase;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Tag(name = "Users", description = "User management — creation is restricted to authenticated ADMINs")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserUseCase userUseCase;

    public UserController(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    @Operation(summary = "Create a user", description = "Requires the ADMIN role — there is no public self-registration.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN"),
            @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    @PostMapping
    public ResponseEntity<UserResponseDto> create(@Valid @RequestBody UserRequestDto request) {
        UserResponseDto created = userUseCase.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Get a user by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(userUseCase.getUserById(id));
    }

    @Operation(summary = "Get a user by email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping(params = "email")
    public ResponseEntity<UserResponseDto> getByEmail(@RequestParam String email) {
        return ResponseEntity.ok(userUseCase.getUserByEmail(email));
    }

    @Operation(summary = "Delete a user by id")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userUseCase.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List a user's transactions",
            description = "Without start/end, returns all of the user's transactions. With start/end, "
                    + "optionally filtered by category type (INCOME or EXPENSE).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponseDto>> transactions(
            @PathVariable UUID id,
            @Parameter(description = "Period start (inclusive), required together with end")
            @RequestParam(required = false) LocalDateTime start,
            @Parameter(description = "Period end (inclusive), required together with start")
            @RequestParam(required = false) LocalDateTime end,
            @Parameter(description = "Optional category type filter, only applied when start/end are set")
            @RequestParam(required = false) Category.Type type) {
        if (start == null || end == null) {
            return ResponseEntity.ok(userUseCase.getUserTransactions(id));
        }
        if (type != null) {
            return ResponseEntity.ok(userUseCase.getUserTransactionsByTypeBetween(id, type, start, end));
        }
        return ResponseEntity.ok(userUseCase.getUserTransactionsBetween(id, start, end));
    }
}
