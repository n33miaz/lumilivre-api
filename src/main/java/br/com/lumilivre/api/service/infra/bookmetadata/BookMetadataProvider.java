package br.com.lumilivre.api.service.infra.bookmetadata;

import java.util.Optional;

/**
 * Strategy de busca de metadados externos por ISBN/título/autor. Implementações
 * registradas como Spring beans são compostas pela {@link BookMetadataChain} —
 * a ordem é controlada por {@code lumilivre.book-metadata.providers}.
 *
 * <p>Implementadores devem:
 * <ul>
 *   <li>Encapsular o circuit breaker / retry (Resilience4j) por provider.</li>
 *   <li>Retornar {@link Optional#empty()} em 404 — nunca lançar.</li>
 *   <li>Nunca expor DTOs específicos do provider externo — convertem para
 *       {@link BookMetadata} antes de retornar.</li>
 * </ul>
 */
public interface BookMetadataProvider {

    /** Identificador do provider, ex.: {@code "googleBooks"}, {@code "brasilApi"}. */
    String name();

    /** Busca por ISBN-10/13 (normalizado). */
    Optional<BookMetadata> findByIsbn(String isbn);

    /**
     * Busca fallback por título/autor — usado pelos providers que aceitam essa
     * query. Provider que não suporta retorna {@link Optional#empty()}.
     */
    default Optional<BookMetadata> findByTitleAndAuthor(String title, String author) {
        return Optional.empty();
    }
}
