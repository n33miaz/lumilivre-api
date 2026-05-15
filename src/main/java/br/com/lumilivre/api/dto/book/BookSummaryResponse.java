package br.com.lumilivre.api.dto.book;

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
public class BookSummaryResponse {

    private UUID bookId;
    private String copyCode;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private String genre;
    private String deweyCode;
    private String physicalLocation;
    private LocalizedEnum copyStatus;
}
