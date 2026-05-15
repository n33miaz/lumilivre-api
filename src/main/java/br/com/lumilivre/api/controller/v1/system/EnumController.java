package br.com.lumilivre.api.controller.v1.system;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import br.com.lumilivre.api.dto.v1.comum.EnumResponse;
import br.com.lumilivre.api.enums.AgeRating;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.CoverType;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.enums.PenaltyCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "16. Enums")
@SecurityRequirement(name = "bearerAuth")
public class EnumController {

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @GetMapping("/enums/{tipo}")
    @Operation(summary = "Lista os valores de um Enum",
               description = "Valores: STATUS_LIVRO, STATUS_EMPRESTIMO, PENALIDADE, TIPO_CAPA, CLASSIFICACAO_ETARIA.")
    @ApiResponse(responseCode = "200", description = "Lista de valores retornada com sucesso")
    @ApiResponse(responseCode = "400", description = "Tipo de lista não encontrado", content = @Content)
    public List<EnumResponse> listarEnum(
            @Parameter(description = "O tipo de lista.", example = "STATUS_LIVRO")
            @PathVariable String tipo) {

        return switch (tipo.toUpperCase()) {
            case "STATUS_LIVRO" -> Arrays.stream(BookCopyStatus.values())
                    .map(s -> new EnumResponse(s.getPtBrCode(), s.getStatus()))
                    .collect(Collectors.toList());
            case "STATUS_EMPRESTIMO" -> Arrays.stream(LoanStatus.values())
                    .map(s -> new EnumResponse(s.getPtBrCode(), s.getStatus()))
                    .collect(Collectors.toList());
            case "PENALIDADE" -> Arrays.stream(PenaltyCode.values())
                    .map(s -> new EnumResponse(s.getPtBrCode(), s.getStatus()))
                    .collect(Collectors.toList());
            case "TIPO_CAPA" -> Arrays.stream(CoverType.values())
                    .map(c -> new EnumResponse(c.getPtBrCode(), c.getStatus()))
                    .collect(Collectors.toList());
            case "CLASSIFICACAO_ETARIA" -> Arrays.stream(AgeRating.values())
                    .map(c -> new EnumResponse(c.getPtBrCode(), c.getStatus()))
                    .collect(Collectors.toList());
            default -> throw new IllegalArgumentException("Tipo de lista não encontrado: " + tipo);
        };
    }
}
