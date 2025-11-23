package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.dto.comum.ApiResponse;
import br.com.lumilivre.api.dto.comum.ItemSimplesResponse;
import br.com.lumilivre.api.dto.modulo.ModuloRequest;
import br.com.lumilivre.api.dto.modulo.ModuloResponse;
import br.com.lumilivre.api.dto.modulo.ModuloResumoResponse;
import br.com.lumilivre.api.repository.ModuloRepository;
import br.com.lumilivre.api.service.ModuloService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/modulos")
@Tag(name = "14. Módulos")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'BIBLIOTECARIO')")
@RequiredArgsConstructor
public class ModuloController {

    private final ModuloRepository moduloRepository;
    private final ModuloService moduloService;

    @GetMapping
    @Operation(summary = "Lista todos os módulos (Simples - para Combobox)")
    public ResponseEntity<List<ItemSimplesResponse>> listarTodos() {
        var lista = moduloRepository.findAll().stream()
                .map(m -> new ItemSimplesResponse(m.getId(), m.getNome()))
                .toList();
        return lista.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(lista);
    }

    @GetMapping("/home")
    @Operation(summary = "Lista módulos para a tela principal do admin (com paginação)")
    public ResponseEntity<Page<ModuloResumoResponse>> buscarModulosAdmin(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<ModuloResumoResponse> modulos = moduloService.buscarPorTexto(texto, pageable);
        return modulos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(modulos);
    }

    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastra um novo módulo")
    public ResponseEntity<ModuloResponse> cadastrar(@RequestBody @Valid ModuloRequest dto) {
        return moduloService.cadastrar(dto);
    }

    @PutMapping("/atualizar/{id}")
    @Operation(summary = "Atualiza um módulo existente")
    public ResponseEntity<ModuloResponse> atualizar(@PathVariable Integer id,
            @RequestBody @Valid ModuloRequest dto) {
        return moduloService.atualizar(id, dto);
    }

    @DeleteMapping("/excluir/{id}")
    @Operation(summary = "Exclui um módulo")
    public ResponseEntity<ApiResponse<Void>> excluir(@PathVariable Integer id) {
        return moduloService.excluir(id);
    }

    @GetMapping("/estatisticas-grafico")
    public ResponseEntity<List<br.com.lumilivre.api.dto.comum.EstatisticaGraficoResponse>> getEstatisticasGrafico() {
        return ResponseEntity.ok(moduloService.buscarTotalEmprestimosPorModulo());
    }
}