package br.com.longobucco.personal_finance_app.infra.rest.controllers;

import br.com.longobucco.personal_finance_app.application.dto.auth.LoginRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.auth.RefreshRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.auth.TokenResponseDto;
import br.com.longobucco.personal_finance_app.application.usecase.AuthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Login and token refresh — no authentication required")
@SecurityRequirements
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @Operation(summary = "Log in with email and password",
            description = "Returns a short-lived access token and a longer-lived refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated successfully"),
            @ApiResponse(responseCode = "400", description = "Malformed request body"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authUseCase.login(request));
    }

    @Operation(summary = "Exchange a refresh token for a new access token",
            description = "The refresh token itself is not rotated and is returned unchanged.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New access token issued"),
            @ApiResponse(responseCode = "400", description = "Malformed request body"),
            @ApiResponse(responseCode = "401", description = "Refresh token is missing, expired, invalid or not a refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refresh(@Valid @RequestBody RefreshRequestDto request) {
        return ResponseEntity.ok(authUseCase.refresh(request.refreshToken()));
    }
}
