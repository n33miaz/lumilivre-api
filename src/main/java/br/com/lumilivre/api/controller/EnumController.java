package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.data.EnumDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class EnumController {

    @GetMapping("lumilivre/enums/{tipo}")
    public List<EnumDTO> listarEnum(@PathVariable String tipo) {
        switch (tipo.toUpperCase()) {
            case "STATUS_LIVRO":
                return listarStatusLivros();
            case "STATUS_EMPRESTIMO":
                return listarStatusEmprestimos();
            default:
                throw new IllegalArgumentException("Tipo de enum não encontrado");
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

}
