package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.dto.ItemSimplesDTO;
import br.com.lumilivre.api.dto.ListaModuloDTO;
import br.com.lumilivre.api.dto.requests.ModuloRequestDTO;
import br.com.lumilivre.api.dto.responses.ModuloResponseDTO;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.ModuloRepository;
import br.com.lumilivre.api.service.ModuloService;
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
@RequestMapping("/modulos")
@Tag(name = "14. Módulos")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'BIBLIOTECARIO')")
public class ModuloController {

    @Autowired
    private ModuloRepository moduloRepository;

    @Autowired
    private ModuloService moduloService;

    @GetMapping
    @Operation(summary = "Lista todos os módulos (Simples - para Combobox)")
    public ResponseEntity<List<ItemSimplesDTO>> listarTodos() {
        var lista = moduloRepository.findAll().stream()
                .map(m -> new ItemSimplesDTO(m.getId(), m.getNome()))
                .toList();
        return lista.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(lista);
    }

    @GetMapping("/home")
    @Operation(summary = "Lista módulos para a tela principal do admin (com paginação)")
    public ResponseEntity<Page<ListaModuloDTO>> buscarModulosAdmin(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<ListaModuloDTO> modulos = moduloService.buscarPorTexto(texto, pageable);
        return modulos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(modulos);
    }

    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastra um novo módulo")
    public ResponseEntity<ModuloResponseDTO> cadastrar(@RequestBody @Valid ModuloRequestDTO dto) {
        return moduloService.cadastrar(dto);
    }

    @PutMapping("/atualizar/{id}")
    @Operation(summary = "Atualiza um módulo existente")
    public ResponseEntity<ModuloResponseDTO> atualizar(@PathVariable Integer id,
            @RequestBody @Valid ModuloRequestDTO dto) {
        return moduloService.atualizar(id, dto);
    }

    @DeleteMapping("/excluir/{id}")
    @Operation(summary = "Exclui um módulo")
    public ResponseEntity<ResponseModel> excluir(@PathVariable Integer id) {
        return moduloService.excluir(id);
    }
}