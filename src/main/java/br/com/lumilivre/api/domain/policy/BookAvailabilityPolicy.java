package br.com.lumilivre.api.domain.policy;

import br.com.lumilivre.api.enums.StatusLivro;

/**
 * Regra de disponibilidade de exemplar.
 */
public final class BookAvailabilityPolicy {

    private BookAvailabilityPolicy() {}

    public static void validateAvailable(StatusLivro status) {
        if (status != StatusLivro.AVAILABLE) {
            throw new BookAvailabilityViolationException(
                    "O exemplar não está disponível para empréstimo. Status atual: " + status);
        }
    }

    public static class BookAvailabilityViolationException extends RuntimeException {
        public BookAvailabilityViolationException(String message) {
            super(message);
        }
    }
}
