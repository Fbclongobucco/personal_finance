package br.com.longobucco.personal_finance_app.application.exception;

public class UserAlreadyExistsException extends ApplicationException {

    private UserAlreadyExistsException(String message) {
        super(message);
    }

    public static UserAlreadyExistsException withEmail(String email) {
        return new UserAlreadyExistsException("User email '%s' is already in use".formatted(email));
    }
}
