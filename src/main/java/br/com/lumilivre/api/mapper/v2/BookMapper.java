package br.com.lumilivre.api.mapper.v2;

import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import br.com.lumilivre.api.dto.book.BookListItemProjection;
import br.com.lumilivre.api.dto.book.BookResponse;
import br.com.lumilivre.api.dto.book.BookSummaryResponse;
import br.com.lumilivre.api.dto.common.LocalizedEnum;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.Genre;
import br.com.lumilivre.api.service.EnumLabelResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookMapper {

    private final EnumLabelResolver enumLabels;

    public BookResponse toResponse(Book b, Locale locale) {
        LocalizedEnum ageRating = b.getAgeRating() != null
                ? LocalizedEnum.of(b.getAgeRating(), enumLabels.resolve(b.getAgeRating(), locale))
                : null;
        LocalizedEnum coverType = b.getCoverType() != null
                ? LocalizedEnum.of(b.getCoverType(), enumLabels.resolve(b.getCoverType(), locale))
                : null;

        return BookResponse.builder()
                .id(b.getId())
                .isbn(b.getIsbn())
                .title(b.getTitle())
                .author(b.getAuthor())
                .publisher(b.getPublisher())
                .publicationDate(b.getPublicationDate())
                .pageCount(b.getPageCount())
                .synopsis(b.getSynopsis())
                .coverUrl(b.getCoverUrl())
                .deweyCode(b.getDeweyClassification() != null
                        ? b.getDeweyClassification().getCode() + " - " + b.getDeweyClassification().getDescription()
                        : null)
                .ageRating(ageRating)
                .coverType(coverType)
                .edition(b.getEdition())
                .volume(b.getVolume())
                .rating(b.getRating())
                .genres(Optional.ofNullable(b.getGenres())
                        .orElse(Collections.emptySet())
                        .stream().map(Genre::getName)
                        .collect(Collectors.toSet()))
                .build();
    }

    public BookSummaryResponse toSummary(BookListItemProjection projection, Locale locale) {
        BookCopyStatus statusEnum = null;
        try {
            if (projection.getStatus() != null) {
                statusEnum = BookCopyStatus.valueOf(projection.getStatus());
            }
        } catch (IllegalArgumentException ignored) {
        }

        LocalizedEnum copyStatus = statusEnum != null
                ? LocalizedEnum.of(statusEnum, enumLabels.resolve(statusEnum, locale))
                : null;

        return BookSummaryResponse.builder()
                .copyCode(projection.getCopyCode())
                .isbn(projection.getIsbn())
                .title(projection.getTitle())
                .author(projection.getAuthor())
                .publisher(projection.getPublisher())
                .genre(projection.getGenre())
                .deweyCode(projection.getDeweyCode())
                .physicalLocation(projection.getPhysicalLocation())
                .copyStatus(copyStatus)
                .build();
    }
}
