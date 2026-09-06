package br.com.longobucco.personal_finance_app.core.exception;

public class InvalidCategoryException extends DomainException {

    private InvalidCategoryException(String message) {
        super(message);
    }

    public static InvalidCategoryException blankName() {
        return new InvalidCategoryException("Category name must not be null or blank");
    }

    public static InvalidCategoryException nullType() {
        return new InvalidCategoryException("Category type must not be null");
    }
}
