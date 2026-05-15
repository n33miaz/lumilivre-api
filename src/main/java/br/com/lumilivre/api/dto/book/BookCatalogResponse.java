package br.com.lumilivre.api.dto.book;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookCatalogResponse {

    private String genreName;
    private List<BookCardResponse> books;
}
