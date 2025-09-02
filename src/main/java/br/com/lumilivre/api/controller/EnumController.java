package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.enums.TipoCapa;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.enums.Cdd;
import br.com.lumilivre.api.enums.Turno;
import br.com.lumilivre.api.enums.ClassificacaoEtaria;
import br.com.lumilivre.api.data.EnumDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController

@Tag(name = "3. Enums")
@SecurityRequirement(name = "bearerAuth")

public class EnumController {

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/enums/{tipo}")

    @Operation(summary = "Lista os valores de um Enum específico", description = "Endpoint genérico para obter os valores possíveis de diferentes enums do sistema, formatados como uma lista de DTOs (nome/valor). Útil para preencher selects e filtros no frontend.")
    @ApiResponse(responseCode = "200", description = "Lista de valores retornada com sucesso")
    @ApiResponse(responseCode = "400", description = "Tipo de enum não encontrado", content = @Content)

    public List<EnumDTO> listarEnum(
            @Parameter(description = "O tipo do enum a ser listado. Valores possíveis: STATUS_LIVRO, STATUS_EMPRESTIMO, PENALIDADE, CDD, TURNO, TIPO_CAPA, CLASSIFICACAO_ETARIA.", example = "STATUS_LIVRO")
            @PathVariable String tipo) {
        switch (tipo.toUpperCase()) {
            case "STATUS_LIVRO":
                return listarStatusLivros();
            case "STATUS_EMPRESTIMO":
                return listarStatusEmprestimos();
            case "PENALIDADE":
                return listarPenalidades();
            case "CDD":
                return listarCdd();
            case "TURNO":
                return listarTurno();
            case "TIPO_CAPA":
                return listarTipoCapa();
            case "CLASSIFICACAO_ETARIA":
                return listarClassificacaoEtaria();
            default:
                throw new IllegalArgumentException("Tipo de enum não encontrado: " + tipo);
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

    private List<EnumDTO> listarCdd() {
        return Arrays.stream(Cdd.values())
                .map(c -> new EnumDTO(c.getCode(), c.getDescription()))
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

    private List<EnumDTO> listarTurno() {
        return Arrays.stream(Turno.values())
                .map(c -> new EnumDTO(c.name(), c.getStatus()))
                .collect(Collectors.toList());
    }
}