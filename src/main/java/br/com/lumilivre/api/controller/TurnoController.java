package br.com.lumilivre.api.controller;

import java.util.List;
import br.com.lumilivre.api.dto.comum.ApiResponse;
import br.com.lumilivre.api.dto.comum.ItemSimplesResponse;
import br.com.lumilivre.api.dto.turno.TurnoRequest;
import br.com.lumilivre.api.dto.turno.TurnoResponse;
import br.com.lumilivre.api.dto.turno.TurnoResumoResponse;
import br.com.lumilivre.api.repository.TurnoRepository;
import br.com.lumilivre.api.service.TurnoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/turnos")
@Tag(name = "15. Turnos")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'BIBLIOTECARIO')")
public class TurnoController {

    private final TurnoRepository turnoRepository;
    private final TurnoService turnoService;

    public TurnoController(TurnoRepository turnoRepository, TurnoService turnoService) {
        this.turnoRepository = turnoRepository;
        this.turnoService = turnoService;
    }

    @GetMapping
    @Operation(summary = "Lista todos os turnos (Simples - para Combobox)")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    public ResponseEntity<List<ItemSimplesResponse>> listarTodos() {
        var lista = turnoRepository.findAll().stream()
                .map(t -> new ItemSimplesResponse(t.getId(), t.getNome()))
                .toList();
        return lista.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(lista);
    }

    @GetMapping("/home")
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @Operation(summary = "Lista turnos para a tela principal do admin (com paginação)")
    public ResponseEntity<Page<TurnoResumoResponse>> buscarTurnosAdmin(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<TurnoResumoResponse> turnos = turnoService.buscarPorTexto(texto, pageable);
        return turnos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(turnos);
    }

    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastra um novo turno")
    public ResponseEntity<TurnoResponse> cadastrar(@RequestBody @Valid TurnoRequest dto) {
        return turnoService.cadastrar(dto);
    }

    @PutMapping("/atualizar/{id}")
    @Operation(summary = "Atualiza um turno existente")
    public ResponseEntity<TurnoResponse> atualizar(@PathVariable Integer id,
            @RequestBody @Valid TurnoRequest dto) {
        return turnoService.atualizar(id, dto);
    }

    @DeleteMapping("/excluir/{id}")
    @Operation(summary = "Exclui um turno")
    public ResponseEntity<ApiResponse<Void>> excluir(@PathVariable Integer id) {
        return turnoService.excluir(id);
    }

    @GetMapping("/estatisticas-grafico")
    public ResponseEntity<List<br.com.lumilivre.api.dto.comum.EstatisticaGraficoResponse>> getEstatisticasGrafico() {
        return ResponseEntity.ok(turnoService.buscarTotalEmprestimosPorTurno());
    }
}