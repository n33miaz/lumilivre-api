package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.dto.ItemSimplesDTO;
import br.com.lumilivre.api.dto.ListaTurnoDTO;
import br.com.lumilivre.api.dto.requests.TurnoRequestDTO;
import br.com.lumilivre.api.dto.responses.TurnoResponseDTO;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.TurnoRepository;
import br.com.lumilivre.api.service.TurnoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/turnos")
@Tag(name = "15. Turnos")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'BIBLIOTECARIO')")
public class TurnoController {

    @Autowired
    private TurnoRepository turnoRepository;

    @Autowired
    private TurnoService turnoService;

    @GetMapping
    @Operation(summary = "Lista todos os turnos (Simples - para Combobox)")
    public ResponseEntity<List<ItemSimplesDTO>> listarTodos() {
        var lista = turnoRepository.findAll().stream()
                .map(t -> new ItemSimplesDTO(t.getId(), t.getNome()))
                .toList();
        return lista.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(lista);
    }

    @GetMapping("/home")
    @Operation(summary = "Lista turnos para a tela principal do admin (com paginação)")
    public ResponseEntity<Page<ListaTurnoDTO>> buscarTurnosAdmin(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<ListaTurnoDTO> turnos = turnoService.buscarPorTexto(texto, pageable);
        return turnos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(turnos);
    }

    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastra um novo turno")
    public ResponseEntity<TurnoResponseDTO> cadastrar(@RequestBody @Valid TurnoRequestDTO dto) {
        return turnoService.cadastrar(dto);
    }

    @PutMapping("/atualizar/{id}")
    @Operation(summary = "Atualiza um turno existente")
    public ResponseEntity<TurnoResponseDTO> atualizar(@PathVariable Integer id,
            @RequestBody @Valid TurnoRequestDTO dto) {
        return turnoService.atualizar(id, dto);
    }

    @DeleteMapping("/excluir/{id}")
    @Operation(summary = "Exclui um turno")
    public ResponseEntity<ResponseModel> excluir(@PathVariable Integer id) {
        return turnoService.excluir(id);
    }
}