package br.com.longobucco.personal_finance_app.core.exception;

import java.time.LocalDateTime;

public class InvalidPeriodException extends DomainException {

    private InvalidPeriodException(String message) {
        super(message);
    }

    public static InvalidPeriodException nullBounds() {
        return new InvalidPeriodException("Period start and end must not be null");
    }

    public static InvalidPeriodException startAfterEnd(LocalDateTime start, LocalDateTime end) {
        return new InvalidPeriodException("Period start '%s' must not be after end '%s'".formatted(start, end));
    }
}
