package br.com.longobucco.personal_finance_app.application.usecase;

import br.com.longobucco.personal_finance_app.application.dto.auth.LoginRequestDto;
import br.com.longobucco.personal_finance_app.application.dto.auth.TokenResponseDto;
import br.com.longobucco.personal_finance_app.application.exception.InvalidCredentialsException;
import br.com.longobucco.personal_finance_app.core.domain.User;
import br.com.longobucco.personal_finance_app.core.repository.UserRepository;
import br.com.longobucco.personal_finance_app.core.security.PasswordHasher;
import br.com.longobucco.personal_finance_app.core.security.TokenService;

public class AuthUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;

    public AuthUseCase(UserRepository userRepository, PasswordHasher passwordHasher, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
    }

    public TokenResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByEmail(request.email())
                .filter(candidate -> passwordHasher.matches(request.password(), candidate.getPassword()))
                .orElseThrow(InvalidCredentialsException::badLogin);
        return new TokenResponseDto(tokenService.generateAccessToken(user), tokenService.generateRefreshToken(user));
    }

    public TokenResponseDto refresh(String refreshToken) {
        String email = tokenService.extractEmailIfValidRefreshToken(refreshToken)
                .orElseThrow(InvalidCredentialsException::badRefreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::badRefreshToken);
        return new TokenResponseDto(tokenService.generateAccessToken(user), refreshToken);
    }
}
