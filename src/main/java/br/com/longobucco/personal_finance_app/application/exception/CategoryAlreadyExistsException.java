package br.com.longobucco.personal_finance_app.application.exception;

import br.com.longobucco.personal_finance_app.core.domain.Category;

public class CategoryAlreadyExistsException extends ApplicationException {

    private CategoryAlreadyExistsException(String message) {
        super(message);
    }

    public static CategoryAlreadyExistsException withNameAndType(String name, Category.Type type) {
        return new CategoryAlreadyExistsException(
                "You already have a %s category named '%s'".formatted(type, name));
    }
}
