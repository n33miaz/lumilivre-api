package br.com.lumilivre.api.dto.book;

import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookRequest {

    @Size(max = 20)
    private String isbn;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String author;

    @NotBlank
    private String publisher;

    private LocalDate publicationDate;
    private Integer pageCount;
    private Integer chapterCount;
    private String deweyCode;

    @NotBlank
    private String ageRating;

    private String edition;
    private Integer volume;
    private Integer copyCount;
    private String synopsis;

    @NotBlank
    private String coverType;

    private String coverUrl;
    private Set<String> genres;
    private Double rating;
}
