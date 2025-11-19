package br.com.lumilivre.api.dto.comum;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO genérico para listagens simples (Combobox/Selects)")
public record ItemSimplesResponse(
        @Schema(description = "Identificador do item (pode ser número ou texto)") Object id,

        @Schema(description = "Nome ou descrição do item") String nome) {
}