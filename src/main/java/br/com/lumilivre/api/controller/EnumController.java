package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.enums.TipoCapa;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.dto.EnumDTO;
import br.com.lumilivre.api.enums.ClassificacaoEtaria;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Tag(name = "3. Enums")
@SecurityRequirement(name = "bearerAuth")
public class EnumController {

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/enums/{tipo}")
    @Operation(summary = "Lista os valores de um Enum", description = "Endpoint para obter listas de valores de Enums estáticos.")
    @ApiResponse(responseCode = "200", description = "Lista de valores retornada com sucesso")
    @ApiResponse(responseCode = "400", description = "Tipo de lista não encontrado", content = @Content)
    public List<EnumDTO> listarEnum(
            @Parameter(description = "O tipo de lista. Valores: STATUS_LIVRO, STATUS_EMPRESTIMO, PENALIDADE, TIPO_CAPA, CLASSIFICACAO_ETARIA.", example = "STATUS_LIVRO") @PathVariable String tipo) {

        switch (tipo.toUpperCase()) {
            case "STATUS_LIVRO":
                return listarStatusLivros();
            case "STATUS_EMPRESTIMO":
                return listarStatusEmprestimos();
            case "PENALIDADE":
                return listarPenalidades();
            case "TIPO_CAPA":
                return listarTipoCapa();
            case "CLASSIFICACAO_ETARIA":
                return listarClassificacaoEtaria();
            default:
                throw new IllegalArgumentException("Tipo de lista não encontrado: " + tipo);
        }
    }

    private List<EnumDTO> listarStatusLivros() {
        return Arrays.stream(StatusLivro.values())
                .map(s -> new EnumDTO(s.name(), s.getStatus()))
                .collect(Collectors.toList());
    }

    private List<EnumDTO> listarStatusEmprestimos() {
        return Arrays.stream(StatusEmprestimo.values())
                .map(s -> new EnumDTO(s.name(), s.getStatus()))
                .collect(Collectors.toList());
    }

    private List<EnumDTO> listarPenalidades() {
        return Arrays.stream(Penalidade.values())
                .map(s -> new EnumDTO(s.name(), s.getStatus()))
                .collect(Collectors.toList());
    }

    private List<EnumDTO> listarClassificacaoEtaria() {
        return Arrays.stream(ClassificacaoEtaria.values())
                .map(c -> new EnumDTO(c.name(), c.getStatus()))
                .collect(Collectors.toList());
    }

    private List<EnumDTO> listarTipoCapa() {
        return Arrays.stream(TipoCapa.values())
                .map(c -> new EnumDTO(c.name(), c.getStatus()))
                .collect(Collectors.toList());
    }
}