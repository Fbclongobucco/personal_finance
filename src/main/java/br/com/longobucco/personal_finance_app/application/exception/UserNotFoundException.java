package br.com.longobucco.personal_finance_app.application.exception;

import java.util.UUID;

public class UserNotFoundException extends ApplicationException {

    private UserNotFoundException(String message) {
        super(message);
    }

    public static UserNotFoundException withId(UUID id) {
        return new UserNotFoundException("User with id '%s' was not found".formatted(id));
    }

    public static UserNotFoundException withEmail(String email) {
        return new UserNotFoundException("User with email '%s' was not found".formatted(email));
    }
}
