package br.com.longobucco.personal_finance_app.infra.rest.controllers;

import br.com.longobucco.personal_finance_app.application.dto.transaction.TransactionResponseDto;
import br.com.longobucco.personal_finance_app.application.dto.user.UserRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.user.UserResponseDto;
import br.com.longobucco.personal_finance_app.application.dto.category.CategoryResponseDto;
import br.com.longobucco.personal_finance_app.application.usecase.CategoryUseCase;
import br.com.longobucco.personal_finance_app.application.usecase.UserUseCase;
import br.com.longobucco.personal_finance_app.core.domain.Category;
import br.com.longobucco.personal_finance_app.core.domain.User;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Tag(name = "Users", description = "User management — creation here is restricted to authenticated ADMINs; "
        + "for public self-registration, see POST /auth/register")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserUseCase userUseCase;
    private final CategoryUseCase categoryUseCase;

    public UserController(UserUseCase userUseCase, CategoryUseCase categoryUseCase) {
        this.userUseCase = userUseCase;
        this.categoryUseCase = categoryUseCase;
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

    @Operation(summary = "Get a user by id", description = "Callers may only fetch their own user, unless they are an ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "403", description = "Caller is not the owner and not an ADMIN"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getById(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userUseCase.getUserById(id, currentUser));
    }

    @Operation(summary = "Get a user by email", description = "Callers may only fetch their own user, unless they are an ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "403", description = "Caller is not the owner and not an ADMIN"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping(params = "email")
    public ResponseEntity<UserResponseDto> getByEmail(@RequestParam String email, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userUseCase.getUserByEmail(email, currentUser));
    }

    @Operation(summary = "Delete a user by id",
            description = "Deletes the user together with everything they own — their transactions and their "
                    + "categories. Callers may only delete their own user, unless they are an ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User and all their transactions and categories deleted"),
            @ApiResponse(responseCode = "403", description = "Caller is not the owner and not an ADMIN"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        userUseCase.deleteUser(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List a user's transactions",
            description = "Without start/end, returns all of the user's transactions. With start/end, "
                    + "optionally filtered by category type (INCOME or EXPENSE). Callers may only list their own "
                    + "transactions, unless they are an ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transactions found"),
            @ApiResponse(responseCode = "400", description = "Period start is after end"),
            @ApiResponse(responseCode = "403", description = "Caller is not the owner and not an ADMIN"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponseDto>> transactions(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "Period start (inclusive), required together with end")
            @RequestParam(required = false) LocalDateTime start,
            @Parameter(description = "Period end (inclusive), required together with start")
            @RequestParam(required = false) LocalDateTime end,
            @Parameter(description = "Optional category type filter, only applied when start/end are set")
            @RequestParam(required = false) Category.Type type) {
        if (start == null || end == null) {
            return ResponseEntity.ok(userUseCase.getUserTransactions(id, currentUser));
        }
        if (type != null) {
            return ResponseEntity.ok(userUseCase.getUserTransactionsByTypeBetween(id, type, start, end, currentUser));
        }
        return ResponseEntity.ok(userUseCase.getUserTransactionsBetween(id, start, end, currentUser));
    }

    @Operation(summary = "List a user's categories",
            description = "The categories this user owns, optionally narrowed by type and by a case-insensitive "
                    + "fragment of the name. Callers may only list their own, unless they are an ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categories listed"),
            @ApiResponse(responseCode = "403", description = "Caller is not the owner and not an ADMIN")
    })
    @GetMapping("/{id}/categories")
    public ResponseEntity<List<CategoryResponseDto>> categories(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "Optional category type filter")
            @RequestParam(required = false) Category.Type type,
            @Parameter(description = "Optional case-insensitive fragment of the category name")
            @RequestParam(required = false) String name) {
        return ResponseEntity.ok(categoryUseCase.searchCategories(List.of(id), type, name, currentUser));
    }
}
