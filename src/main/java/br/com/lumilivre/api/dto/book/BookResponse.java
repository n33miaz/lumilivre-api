package br.com.lumilivre.api.dto.book;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import br.com.lumilivre.api.dto.common.LocalizedEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {

    private UUID id;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private LocalDate publicationDate;
    private Integer pageCount;
    private String synopsis;
    private String coverUrl;
    private String deweyCode;
    private LocalizedEnum ageRating;
    private LocalizedEnum coverType;
    private String edition;
    private Integer volume;
    private Double rating;
    private Set<String> genres;
}
