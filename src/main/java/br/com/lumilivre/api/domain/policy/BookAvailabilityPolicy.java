package br.com.lumilivre.api.domain.policy;

import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.exception.custom.MessageKeyedException;

public final class BookAvailabilityPolicy {

    private BookAvailabilityPolicy() {}

    public static void validateAvailable(BookCopyStatus status) {
        if (status != BookCopyStatus.AVAILABLE) {
            throw new BookAvailabilityViolationException("book.copy.not-available", status);
        }
    }

    public static class BookAvailabilityViolationException extends RuntimeException implements MessageKeyedException {
        private final String messageKey;
        private final Object[] messageArgs;

        public BookAvailabilityViolationException(String key, Object... args) {
            super(key);
            this.messageKey = key;
            this.messageArgs = args;
        }

        @Override
        public boolean hasI18nKey() {
            return true;
        }

        @Override
        public String getMessageKey() {
            return messageKey;
        }

        @Override
        public Object[] getMessageArgs() {
            return messageArgs;
        }
    }
}
