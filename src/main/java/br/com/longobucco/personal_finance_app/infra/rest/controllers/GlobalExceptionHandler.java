package br.com.longobucco.personal_finance_app.infra.rest.controllers;

import br.com.longobucco.personal_finance_app.application.exception.CategoryNotFoundException;
import br.com.longobucco.personal_finance_app.application.exception.ForbiddenException;
import br.com.longobucco.personal_finance_app.application.exception.InvalidCredentialsException;
import br.com.longobucco.personal_finance_app.application.exception.TransactionNotFoundException;
import br.com.longobucco.personal_finance_app.application.exception.UserAlreadyExistsException;
import br.com.longobucco.personal_finance_app.application.exception.UserNotFoundException;
import br.com.longobucco.personal_finance_app.core.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps application/domain exceptions (which are plain RuntimeExceptions with no framework
 * dependency) to the HTTP status codes documented on the controllers' @ApiResponses. Without this,
 * they all fall through to Spring Boot's default handling as 500 Internal Server Error.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({UserNotFoundException.class, CategoryNotFoundException.class, TransactionNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ProblemDetail handleAlreadyExists(UserAlreadyExistsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
