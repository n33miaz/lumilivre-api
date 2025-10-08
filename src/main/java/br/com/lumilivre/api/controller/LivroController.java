package br.com.lumilivre.api.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import br.com.lumilivre.api.data.ListaLivroDTO;
import br.com.lumilivre.api.data.LivroAgrupadoDTO;
import br.com.lumilivre.api.data.LivroDTO;
import br.com.lumilivre.api.data.LivroResponseMobileGeneroDTO;
import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.LivroService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

@RestController
@RequestMapping("/livros")
@Tag(name = "7. Livros")
@SecurityRequirement(name = "bearerAuth")
public class LivroController {

    @Autowired
    private LivroService ls;

    // ==================== MÉTODOS GET ====================
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/home")
    @Operation(summary = "Lista livros para a tela principal do admin")
    public ResponseEntity<Page<ListaLivroDTO>> listarParaAdmin(
            @Parameter(description = "Texto para busca genérica") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<ListaLivroDTO> livros = ls.buscarParaListaAdmin(pageable);
        return livros.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(livros);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/home/agrupado")
    @Operation(summary = "Lista livros agrupados por ISBN com contagem de exemplares")
    public ResponseEntity<Page<LivroAgrupadoDTO>> listarAgrupadoParaAdmin(
            @Parameter(description = "Texto para busca por nome ou ISBN") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<LivroAgrupadoDTO> livros = ls.buscarLivrosAgrupados(pageable, texto);
        return livros.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(livros);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @GetMapping("/{isbn}")
    @Operation(summary = "Busca um livro específico pelo ISBN")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Livro encontrado", content = @Content(schema = @Schema(implementation = LivroModel.class))),
            @ApiResponse(responseCode = "404", description = "Nenhum livro encontrado para o ISBN fornecido")
    })
    public ResponseEntity<LivroModel> buscarPorIsbn(
            @Parameter(description = "ISBN do livro a ser buscado") @PathVariable String isbn) {
        return ls.findByIsbn(isbn);
    }

    @PreAuthorize("hasAnyRole('ALUNO')")
    @GetMapping("/genero/{genero}")
    @Operation(summary = "Busca livros como lista")
    public ResponseEntity<List<LivroResponseMobileGeneroDTO>> listarPorGenero(@PathVariable String genero) {
        return ls.listarPorGenero(genero);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO', 'ALUNO')")
    @GetMapping("/buscar")
    @Operation(summary = "Busca livros com paginação e filtro de texto")
    public ResponseEntity<Page<LivroModel>> buscarPorTexto(
            @Parameter(description = "Texto para busca genérica") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<LivroModel> livros = ls.buscarPorTexto(texto, pageable);
        return livros.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(livros);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO', 'ALUNO')")
    @GetMapping("/buscar/avancado")
    @Operation(summary = "Busca avançada e paginada de livros")
    public ResponseEntity<Page<LivroModel>> buscarAvancado(
            @Parameter(description = "Nome parcial do livro") @RequestParam(required = false) String nome,
            @Parameter(description = "ISBN exato do livro") @RequestParam(required = false) String isbn,
            @Parameter(description = "Nome parcial do autor") @RequestParam(required = false) String autor,
            @Parameter(description = "Nome parcial do gênero") @RequestParam(required = false) String genero,
            @Parameter(description = "Nome parcial da editora") @RequestParam(required = false) String editora,
            Pageable pageable) {
        Page<LivroModel> livros = ls.buscarAvancado(nome, isbn, autor, genero, editora, pageable);
        return livros.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(livros);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @GetMapping("/disponiveis")
    @Operation(summary = "Lista livros com exemplares disponíveis")
    public ResponseEntity<Iterable<LivroModel>> listarDisponiveis() {
        Iterable<LivroModel> livrosDisponiveis = ls.buscarLivrosDisponiveis();
        return !livrosDisponiveis.iterator().hasNext() ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(livrosDisponiveis);
    }

    // ==================== MÉTODOS POST ====================

    // ✅ CADASTRO SIMPLES (APENAS DADOS - JSON)
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastra um novo livro (apenas dados)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Livro cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos")
    })
    public ResponseEntity<?> cadastrarLivro(@RequestBody LivroDTO livroDTO) {
        return ls.cadastrar(livroDTO, null);
    }

    // ✅ UPLOAD DE CAPA (PARA LIVRO JÁ EXISTENTE)
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping(value = "/{isbn}/capa", consumes = "multipart/form-data")
    @Operation(summary = "Faz upload da capa para um livro existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Capa atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado")
    })
    public ResponseEntity<?> uploadCapa(
            @Parameter(description = "ISBN do livro") @PathVariable String isbn,
            @Parameter(description = "Arquivo de imagem da capa") @RequestParam("file") MultipartFile file) {
        return ls.uploadCapa(isbn, file);
    }

    // ==================== MÉTODOS PUT ====================
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("/{isbn}")
    @Operation(summary = "Atualiza um livro existente")
    public ResponseEntity<?> atualizar(
            @PathVariable String isbn,
            @RequestBody LivroDTO livroDTO) {
        livroDTO.setIsbn(isbn);
        return ls.atualizar(livroDTO, null);
    }

    // ==================== MÉTODOS DELETE ====================
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @DeleteMapping("/{isbn}")
    @Operation(summary = "Exclui um livro")
    public ResponseEntity<ResponseModel> excluir(
            @Parameter(description = "ISBN do livro a ser excluído") @PathVariable String isbn) {
        return ls.excluir(isbn);
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping("/{isbn}/com-exemplares")
    @Operation(summary = "Exclui um livro e seus exemplares")
    public ResponseEntity<?> excluirComExemplares(
            @Parameter(description = "ISBN do livro e exemplares a serem excluídos") @PathVariable String isbn) {
        return ls.excluirLivroComExemplares(isbn);
    }
}