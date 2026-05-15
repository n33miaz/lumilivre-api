package br.com.lumilivre.api.dto.book;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import br.com.lumilivre.api.dto.common.LocalizedEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BookCopyResponse {

    private String copyCode;
    private LocalizedEnum status;
    private UUID bookId;
    private String isbn;
    private String title;
    private String author;
    private String publisher;
    private String genre;
    private String deweyCode;
    private String physicalLocation;
}
