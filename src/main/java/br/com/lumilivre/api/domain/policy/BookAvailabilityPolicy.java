package br.com.lumilivre.api.domain.policy;

import br.com.lumilivre.api.enums.BookCopyStatus;

public final class BookAvailabilityPolicy {

    private BookAvailabilityPolicy() {}

    public static void validateAvailable(BookCopyStatus status) {
        if (status != BookCopyStatus.AVAILABLE) {
            throw new BookAvailabilityViolationException(
                    "Book copy is not available for loan. Current status: " + status);
        }
    }

    public static class BookAvailabilityViolationException extends RuntimeException {
        public BookAvailabilityViolationException(String message) {
            super(message);
        }
    }
}
