package br.com.longobucco.personal_finance_app.application.exception;

public class InvalidCredentialsException extends ApplicationException {

    private InvalidCredentialsException(String message) {
        super(message);
    }

    public static InvalidCredentialsException badLogin() {
        return new InvalidCredentialsException("Invalid email or password");
    }

    public static InvalidCredentialsException badRefreshToken() {
        return new InvalidCredentialsException("Refresh token is missing, expired, invalid or not a refresh token");
    }
}
