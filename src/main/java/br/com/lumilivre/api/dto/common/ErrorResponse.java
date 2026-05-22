package br.com.lumilivre.api.dto.common;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error payload returned by LumiLivre API.")
public class ErrorResponse {

    @Builder.Default
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Server-side timestamp when the error response was generated.", example = "2026-05-22T10:00:00")
    private LocalDateTime timestamp = LocalDateTime.now();

    @Schema(description = "HTTP status code.", example = "400")
    private int status;

    @Schema(description = "Short localized error title.", example = "Bad Request")
    private String error;

    @Schema(description = "Localized human-readable error message.", example = "The request payload is invalid.")
    private String message;

    @Schema(description = "Request path that produced the error.", example = "/api/books")
    private String path;

    @Schema(description = "Correlation identifier propagated in logs and response headers.", example = "a1b2c3d4-e5f6-7890")
    private String correlationId;

    @Schema(description = "Field-level validation errors keyed by field name.")
    private Map<String, String> violations;
}
