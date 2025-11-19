package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.dto.ItemSimplesDTO;
import br.com.lumilivre.api.repository.CddRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cdd")
@Tag(name = "12. CDD")
public class CddController {

    @Autowired
    private CddRepository cddRepository;

    @GetMapping
    @Operation(summary = "Lista todas as classificações CDD disponíveis")
    public ResponseEntity<List<ItemSimplesDTO>> listarTodos() {
        var lista = cddRepository.findAll().stream()
                .map(cdd -> new ItemSimplesDTO(cdd.getCodigo(), cdd.getDescricao()))
                .toList();

        return ResponseEntity.ok(lista);
    }
}