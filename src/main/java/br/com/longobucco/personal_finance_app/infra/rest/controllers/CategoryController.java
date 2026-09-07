package br.com.longobucco.personal_finance_app.infra.rest.controllers;

import br.com.longobucco.personal_finance_app.application.dto.category.CategoryRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.category.CategoryResponseDto;
import br.com.longobucco.personal_finance_app.application.dto.category.CategoryUpdateRequestDto;
import br.com.longobucco.personal_finance_app.application.usecase.CategoryUseCase;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Categories", description = "Income/expense categories used to classify transactions — each user "
        + "creates and manages their own categories, which only they (or an ADMIN, for viewing) can access. "
        + "A category the caller may not see is reported as 404, not 403, so ids cannot be probed.")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryUseCase categoryUseCase;

    public CategoryController(CategoryUseCase categoryUseCase) {
        this.categoryUseCase = categoryUseCase;
    }

    @Operation(summary = "Create a category", description = "Creates the category owned by the authenticated caller. "
            + "Every account starts with a default set, so this is for the ones beyond those.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Caller already has a category with this name and type")
    })
    @PostMapping
    public ResponseEntity<CategoryResponseDto> create(@Valid @RequestBody CategoryRequestDto request,
                                                       @AuthenticationPrincipal User currentUser) {
        CategoryResponseDto created = categoryUseCase.createCategory(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Get a category by id", description = "Callers may only fetch their own categories, unless they are an ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category found"),
            @ApiResponse(responseCode = "404", description = "Category not found, or not visible to the caller")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> getById(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(categoryUseCase.getCategoryById(id, currentUser));
    }

    @Operation(summary = "Search categories",
            description = "Without userId, returns the caller's own categories. With one or more userId values, "
                    + "returns those users' categories — a caller may only ask for their own unless they are an "
                    + "ADMIN. With allUsers=true, returns every user's categories, which requires the ADMIN role. "
                    + "Results can be narrowed by type and by a case-insensitive fragment of the name, and come "
                    + "back ordered by type and then name.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categories listed"),
            @ApiResponse(responseCode = "403", description = "Caller asked for another user's categories without "
                    + "being an ADMIN, or asked for allUsers without the ADMIN role")
    })
    @GetMapping
    public ResponseEntity<List<CategoryResponseDto>> search(
            @Parameter(description = "Ids of the users whose categories are returned; repeatable. "
                    + "Defaults to the caller.")
            @RequestParam(required = false) List<UUID> userId,
            @AuthenticationPrincipal User currentUser,
            @Parameter(description = "Optional category type filter")
            @RequestParam(required = false) Category.Type type,
            @Parameter(description = "Optional case-insensitive fragment of the category name")
            @RequestParam(required = false) String name,
            @Parameter(description = "ADMIN only: return every user's categories, ignoring userId")
            @RequestParam(required = false, defaultValue = "false") boolean allUsers) {
        if (allUsers) {
            return ResponseEntity.ok(categoryUseCase.searchEveryUsersCategories(type, name, currentUser));
        }
        return ResponseEntity.ok(categoryUseCase.searchCategories(userId, type, name, currentUser));
    }

    @Operation(summary = "Rename a category",
            description = "Only the name can change — the type is fixed for the life of the category, since "
                    + "flipping it would invert the effect of every transaction already classified with it. Only "
                    + "the category's own user may rename it.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category renamed"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "403", description = "Caller is not the category's own user"),
            @ApiResponse(responseCode = "404", description = "Category not found, or not visible to the caller"),
            @ApiResponse(responseCode = "409", description = "Caller already has a category with this name and type")
    })
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> rename(@PathVariable UUID id,
                                                       @Valid @RequestBody CategoryUpdateRequestDto request,
                                                       @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(categoryUseCase.renameCategory(id, request, currentUser));
    }

    @Operation(summary = "Delete a category by id",
            description = "Only the category's own user may delete it — not even an ADMIN can delete someone "
                    + "else's category.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category deleted"),
            @ApiResponse(responseCode = "403", description = "Caller is not the category's own user"),
            @ApiResponse(responseCode = "404", description = "Category not found, or not visible to the caller"),
            @ApiResponse(responseCode = "409", description = "Category is still referenced by existing transactions")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        categoryUseCase.deleteCategory(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
