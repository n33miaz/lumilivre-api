package br.com.lumilivre.api.mapper;

import java.util.Locale;

import br.com.lumilivre.api.dto.book.BookCopyResponse;
import br.com.lumilivre.api.dto.common.LocalizedEnum;
import br.com.lumilivre.api.model.BookCopy;
import br.com.lumilivre.api.model.Genre;
import br.com.lumilivre.api.service.EnumLabelResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookCopyMapper {

    private final EnumLabelResolver enumLabels;

    public BookCopyResponse toResponse(BookCopy copy, Locale locale) {
        LocalizedEnum status = copy.getStatus() != null
                ? LocalizedEnum.of(copy.getStatus(), enumLabels.resolve(copy.getStatus(), locale))
                : null;

        var book = copy.getBook();
        String genres = book != null && book.getGenres() != null
                ? book.getGenres().stream().map(Genre::getName).reduce((a, b) -> a + ", " + b).orElse("")
                : "";

        return BookCopyResponse.builder()
                .copyCode(copy.getCopyCode())
                .status(status)
                .bookId(book != null ? book.getId() : null)
                .isbn(book != null ? book.getIsbn() : null)
                .title(book != null ? book.getTitle() : null)
                .author(book != null ? book.getAuthor() : null)
                .publisher(book != null ? book.getPublisher() : null)
                .genre(genres)
                .deweyCode(book != null && book.getDeweyClassification() != null
                        ? book.getDeweyClassification().getCode()
                        : null)
                .physicalLocation(copy.getShelfLocation())
                .build();
    }
}
