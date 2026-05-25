package br.com.lumilivre.api.service.infra.bookmetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Composite Pattern + Chain of Responsibility: itera pelos providers ordenados
 * conforme {@code lumilivre.book-metadata.providers} e faz <b>merge</b> de
 * campos faltantes do segundo (terceiro...) provider em cima do primeiro
 * resultado encontrado. Default merge-strategy é {@code fill-missing}.
 *
 * Não conhece formato externo — só {@link BookMetadata}.
 */
@Component
public class BookMetadataChain {

    private static final Logger log = LoggerFactory.getLogger(BookMetadataChain.class);

    private final Map<String, BookMetadataProvider> providersByName;
    private final List<String> providerOrder;

    public BookMetadataChain(
            List<BookMetadataProvider> providers,
            @Value("${lumilivre.book-metadata.providers:googleBooks,brasilApi,openLibrary}") String orderCsv) {
        this.providersByName = new LinkedHashMap<>();
        for (BookMetadataProvider p : providers) {
            providersByName.put(p.name(), p);
        }
        this.providerOrder = Arrays.stream(orderCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public Optional<BookMetadata> findByIsbn(String isbn) {
        return findMerged(p -> p.findByIsbn(isbn));
    }

    public Optional<BookMetadata> findByTitleAndAuthor(String title, String author) {
        return findMerged(p -> p.findByTitleAndAuthor(title, author));
    }

    private Optional<BookMetadata> findMerged(ProviderQuery query) {
        BookMetadata accumulator = null;
        List<String> contributors = new ArrayList<>();

        for (String name : providerOrder) {
            BookMetadataProvider provider = providersByName.get(name);
            if (provider == null) {
                log.debug("Provider '{}' não encontrado no contexto Spring; ignorando.", name);
                continue;
            }
            Optional<BookMetadata> result;
            try {
                result = query.run(provider);
            } catch (Exception e) {
                log.warn("Provider {} falhou silenciosamente: {}", name, e.getMessage());
                continue;
            }
            if (result.isEmpty()) {
                continue;
            }
            contributors.add(name);
            accumulator = (accumulator == null) ? result.get() : merge(accumulator, result.get());
            if (isComplete(accumulator)) {
                break;
            }
        }

        if (accumulator != null && log.isDebugEnabled()) {
            log.debug("Book metadata composto a partir de providers={}", contributors);
        }
        return Optional.ofNullable(accumulator);
    }

    /** Mantém o valor do primeiro provider quando presente; preenche faltantes a partir do segundo. */
    BookMetadata merge(BookMetadata primary, BookMetadata secondary) {
        return new BookMetadata(
                primary.isbn() != null ? primary.isbn() : secondary.isbn(),
                primary.title() != null ? primary.title() : secondary.title(),
                primary.author() != null ? primary.author() : secondary.author(),
                primary.publisher() != null ? primary.publisher() : secondary.publisher(),
                primary.synopsis() != null ? primary.synopsis() : secondary.synopsis(),
                primary.publicationDate() != null ? primary.publicationDate() : secondary.publicationDate(),
                primary.pageCount() != null && primary.pageCount() > 0 ? primary.pageCount() : secondary.pageCount(),
                primary.coverUrl() != null ? primary.coverUrl() : secondary.coverUrl(),
                primary.rating() != null ? primary.rating() : secondary.rating(),
                primary.categories() != null && !primary.categories().isEmpty() ? primary.categories() : secondary.categories(),
                primary.providerName() + "+" + secondary.providerName()
        );
    }

    private boolean isComplete(BookMetadata m) {
        return m.title() != null && m.author() != null && m.publisher() != null
                && m.synopsis() != null && m.coverUrl() != null && m.publicationDate() != null;
    }

    @FunctionalInterface
    private interface ProviderQuery {
        Optional<BookMetadata> run(BookMetadataProvider provider);
    }
}
