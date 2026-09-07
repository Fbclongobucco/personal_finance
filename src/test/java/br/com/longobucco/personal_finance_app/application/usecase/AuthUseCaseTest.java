package br.com.longobucco.personal_finance_app.application.usecase;

import br.com.longobucco.personal_finance_app.application.dto.auth.LoginRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.auth.TokenResponseDto;
import br.com.longobucco.personal_finance_app.application.exception.InvalidCredentialsException;
import br.com.longobucco.personal_finance_app.core.domain.User;
import br.com.longobucco.personal_finance_app.core.repository.UserRepository;
import br.com.longobucco.personal_finance_app.core.security.PasswordHasher;
import br.com.longobucco.personal_finance_app.core.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenService tokenService;

    private AuthUseCase authUseCase;

    @BeforeEach
    void setUp() {
        authUseCase = new AuthUseCase(userRepository, passwordHasher, tokenService);
    }

    private User validUser() {
        return User.createUser("John Doe", "john.doe@example.com", "11987654321", "hashed-secret",
                new BigDecimal("100.00"));
    }

    @Test
    void loginReturnsTokenPairWhenCredentialsAreValid() {
        User user = validUser();
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("secret123", "hashed-secret")).thenReturn(true);
        when(tokenService.generateAccessToken(user)).thenReturn("access-token");
        when(tokenService.generateRefreshToken(user)).thenReturn("refresh-token");

        TokenResponseDto result = authUseCase.login(new LoginRequestDto("john.doe@example.com", "secret123"));

        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
    }

    @Test
    void loginThrowsInvalidCredentialsExceptionWhenEmailNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authUseCase.login(new LoginRequestDto("missing@example.com", "secret123")));
    }

    @Test
    void loginThrowsInvalidCredentialsExceptionWhenPasswordDoesNotMatch() {
        User user = validUser();
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("wrong-password", "hashed-secret")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authUseCase.login(new LoginRequestDto("john.doe@example.com", "wrong-password")));
    }

    @Test
    void refreshReturnsNewAccessTokenAndSameRefreshTokenWhenValid() {
        User user = validUser();
        when(tokenService.extractEmailIfValidRefreshToken("refresh-token"))
                .thenReturn(Optional.of("john.doe@example.com"));
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
        when(tokenService.generateAccessToken(user)).thenReturn("new-access-token");

        TokenResponseDto result = authUseCase.refresh("refresh-token");

        assertEquals("new-access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
    }

    @Test
    void refreshThrowsInvalidCredentialsExceptionWhenTokenIsNotAValidRefreshToken() {
        when(tokenService.extractEmailIfValidRefreshToken("garbage")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authUseCase.refresh("garbage"));
    }

    @Test
    void refreshThrowsInvalidCredentialsExceptionWhenUserNoLongerExists() {
        when(tokenService.extractEmailIfValidRefreshToken("refresh-token"))
                .thenReturn(Optional.of("gone@example.com"));
        when(userRepository.findByEmail("gone@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authUseCase.refresh("refresh-token"));
    }
}
