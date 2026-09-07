package br.com.longobucco.personal_finance_app.infra.rest.controllers;

import br.com.longobucco.personal_finance_app.core.domain.User;
import br.com.longobucco.personal_finance_app.core.repository.UserRepository;
import br.com.longobucco.personal_finance_app.core.security.PasswordHasher;
import br.com.longobucco.personal_finance_app.infra.rest.security.JwtService;
import br.com.longobucco.personal_finance_app.infra.rest.security.LoginRequest;
import br.com.longobucco.personal_finance_app.infra.rest.security.RefreshRequest;
import br.com.longobucco.personal_finance_app.infra.rest.security.TokenResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Tag(name = "Auth", description = "Login and token refresh — no authentication required")
@SecurityRequirements
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordHasher passwordHasher, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.jwtService = jwtService;
    }

    @Operation(summary = "Log in with email and password",
            description = "Returns a short-lived access token and a longer-lived refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated successfully"),
            @ApiResponse(responseCode = "400", description = "Malformed request body"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        Optional<User> user = userRepository.findByEmail(request.email());
        if (user.isEmpty() || !passwordHasher.matches(request.password(), user.get().getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User authenticated = user.get();
        TokenResponse tokens = new TokenResponse(jwtService.generateAccessToken(authenticated),
                jwtService.generateRefreshToken(authenticated));
        return ResponseEntity.ok(tokens);
    }

    @Operation(summary = "Exchange a refresh token for a new access token",
            description = "The refresh token itself is not rotated and is returned unchanged.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New access token issued"),
            @ApiResponse(responseCode = "400", description = "Malformed request body"),
            @ApiResponse(responseCode = "401", description = "Refresh token is missing, expired, invalid or not a refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        try {
            Claims claims = jwtService.parseClaims(request.refreshToken());
            if (!jwtService.isRefreshToken(claims)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            Optional<User> user = userRepository.findByEmail(claims.getSubject());
            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            String newAccessToken = jwtService.generateAccessToken(user.get());
            return ResponseEntity.ok(new TokenResponse(newAccessToken, request.refreshToken()));
        } catch (JwtException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
