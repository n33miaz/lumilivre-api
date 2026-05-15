package br.com.lumilivre.api.dto.book;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookCardResponse {

    private UUID id;
    private String title;
    private String author;
    private String coverUrl;
    private Double rating;
}
