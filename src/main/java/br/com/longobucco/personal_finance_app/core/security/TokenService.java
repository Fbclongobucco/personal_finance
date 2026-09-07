package br.com.longobucco.personal_finance_app.core.security;

import br.com.longobucco.personal_finance_app.core.domain.User;

import java.util.Optional;

public interface TokenService {

    String generateAccessToken(User user);

    String generateRefreshToken(User user);

    /**
     * Returns the subject (email) of the token, if and only if it is a well-formed, unexpired
     * access token. Empty for any other reason (garbled, expired, wrong type, ...).
     */
    Optional<String> extractEmailIfValidAccessToken(String token);

    /**
     * Same as {@link #extractEmailIfValidAccessToken(String)}, but for refresh tokens.
     */
    Optional<String> extractEmailIfValidRefreshToken(String token);
}
