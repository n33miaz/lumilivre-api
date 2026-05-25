package br.com.lumilivre.api.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.dto.metadata.AuthorSummaryResponse;
import br.com.lumilivre.api.repository.BookRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MetadataService {

    private final BookRepository bookRepository;

    public Page<AuthorSummaryResponse> authors(String query, Pageable pageable) {
        List<AuthorSummaryResponse> authors = bookRepository.countByAutor()
                .stream()
                .map(this::toAuthorSummary)
                .filter(author -> matchesQuery(author.name(), query))
                .toList();
        int start = Math.min((int) pageable.getOffset(), authors.size());
        int end = Math.min(start + pageable.getPageSize(), authors.size());
        return new PageImpl<>(authors.subList(start, end), pageable, authors.size());
    }

    private AuthorSummaryResponse toAuthorSummary(Map<String, Object> row) {
        Object total = row.get("total");
        return new AuthorSummaryResponse(
                String.valueOf(row.get("autor")),
                total instanceof Number number ? number.longValue() : 0L);
    }

    private boolean matchesQuery(String value, String query) {
        if (isBlank(query)) {
            return true;
        }
        return value != null && value.toLowerCase(Locale.ROOT)
                .contains(query.trim().toLowerCase(Locale.ROOT));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
