package br.com.longobucco.personal_finance_app.application.exception;

public class ForbiddenException extends ApplicationException {

    private ForbiddenException(String message) {
        super(message);
    }

    public static ForbiddenException notOwner() {
        return new ForbiddenException("You are not allowed to access this resource");
    }

    public static ForbiddenException notAdmin() {
        return new ForbiddenException("This operation requires the ADMIN role");
    }
}
