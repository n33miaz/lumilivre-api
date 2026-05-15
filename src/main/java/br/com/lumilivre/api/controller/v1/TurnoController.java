package br.com.lumilivre.api.controller.v1;

import java.util.List;
import br.com.lumilivre.api.dto.v1.comum.ApiResponse;
import br.com.lumilivre.api.dto.v1.comum.ItemSimplesResponse;
import br.com.lumilivre.api.dto.v1.turno.TurnoRequest;
import br.com.lumilivre.api.dto.v1.turno.TurnoResponse;
import br.com.lumilivre.api.dto.v1.turno.TurnoResumoResponse;
import br.com.lumilivre.api.repository.StudyShiftRepository;
import br.com.lumilivre.api.service.StudyShiftService;
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
@Tag(name = "10. Turnos")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
public class TurnoController {

    private final StudyShiftRepository studyShiftRepository;
    private final StudyShiftService studyShiftService;

    public TurnoController(StudyShiftRepository studyShiftRepository, StudyShiftService studyShiftService) {
        this.studyShiftRepository = studyShiftRepository;
        this.studyShiftService = studyShiftService;
    }

    @GetMapping
    @Operation(summary = "Lista todos os turnos (Simples - para Combobox)")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    public ResponseEntity<List<ItemSimplesResponse>> listarTodos() {
        var lista = studyShiftRepository.findAll().stream()
                .map(t -> new ItemSimplesResponse(t.getId(), t.getName()))
                .toList();
        return lista.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(lista);
    }

    @GetMapping("/home")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    @Operation(summary = "Lista turnos para a tela principal do admin (com paginação)")
    public ResponseEntity<Page<TurnoResumoResponse>> buscarTurnosAdmin(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<TurnoResumoResponse> turnos = studyShiftService.buscarPorTexto(texto, pageable);
        return turnos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(turnos);
    }

    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastra um novo turno")
    public ResponseEntity<TurnoResponse> cadastrar(@RequestBody @Valid TurnoRequest dto) {
        return studyShiftService.cadastrar(dto);
    }

    @PutMapping("/atualizar/{id}")
    @Operation(summary = "Atualiza um turno existente")
    public ResponseEntity<TurnoResponse> atualizar(@PathVariable Integer id,
            @RequestBody @Valid TurnoRequest dto) {
        return studyShiftService.atualizar(id, dto);
    }

    @DeleteMapping("/excluir/{id}")
    @Operation(summary = "Exclui um turno")
    public ResponseEntity<ApiResponse<Void>> excluir(@PathVariable Integer id) {
        return studyShiftService.excluir(id);
    }

    @GetMapping("/estatisticas-grafico")
    @Operation(summary = "Retorna estatísticas de empréstimos por turno para gráficos")
    public ResponseEntity<List<br.com.lumilivre.api.dto.v1.comum.EstatisticaGraficoResponse>> getEstatisticasGrafico() {
        return ResponseEntity.ok(studyShiftService.buscarTotalEmprestimosPorTurno());
    }
}
