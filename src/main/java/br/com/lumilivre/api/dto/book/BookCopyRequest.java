package br.com.lumilivre.api.dto.book;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookCopyRequest {

    @NotBlank
    private String copyCode;

    private String status;

    @NotNull
    private UUID bookId;

    @NotBlank
    private String physicalLocation;
}
