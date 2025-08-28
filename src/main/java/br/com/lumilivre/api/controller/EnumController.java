package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.enums.TipoCapa;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.enums.Cdd;
import br.com.lumilivre.api.enums.Turno;
import br.com.lumilivre.api.enums.ClassificacaoEtaria;
import br.com.lumilivre.api.data.EnumDTO;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class EnumController {

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/enums/{tipo}")
    public List<EnumDTO> listarEnum(@PathVariable String tipo) {
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
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tipo de enum não encontrado: " + tipo
                );
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
