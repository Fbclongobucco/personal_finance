package br.com.longobucco.personal_finance_app.core.security;

import br.com.longobucco.personal_finance_app.core.domain.User;

import java.util.Optional;

public interface TokenService {

    String generateAccessToken(User user);

    String generateRefreshToken(User user);

    Optional<String> extractEmailIfValidAccessToken(String token);

    Optional<String> extractEmailIfValidRefreshToken(String token);
}
