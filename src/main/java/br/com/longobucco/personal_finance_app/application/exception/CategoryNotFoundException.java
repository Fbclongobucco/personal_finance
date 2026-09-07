package br.com.longobucco.personal_finance_app.application.exception;

import java.util.UUID;

public class CategoryNotFoundException extends ApplicationException {

    private CategoryNotFoundException(String message) {
        super(message);
    }

    public static CategoryNotFoundException withId(UUID id) {
        return new CategoryNotFoundException("Category with id '%s' was not found".formatted(id));
    }
}
