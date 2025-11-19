package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.dto.*;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.LivroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/livros")
@Tag(name = "7. Livros")
@SecurityRequirement(name = "bearerAuth")
public class LivroController {

    private final LivroService livroService;

    public LivroController(LivroService ls) {
        this.livroService = ls;
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/home")
    @Operation(summary = "Lista livros para a tela principal do admin (visão de exemplares)")
    public ResponseEntity<Page<ListaLivroDTO>> listarParaAdmin(Pageable pageable) {
        Page<ListaLivroDTO> livros = livroService.buscarParaListaAdmin(pageable);
        return livros.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(livros);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/home/agrupado")
    @Operation(summary = "Lista livros agrupados por título com contagem de exemplares e busca")
    public ResponseEntity<Page<LivroAgrupadoDTO>> listarAgrupadoParaAdmin(
            @Parameter(description = "Texto para busca por nome ou ISBN") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<LivroAgrupadoDTO> livros = livroService.buscarLivrosAgrupados(pageable, texto);
        return livros.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(livros);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @GetMapping("/{id}")
    @Operation(summary = "Busca os detalhes de um livro específico pelo seu ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Livro encontrado", content = @Content(schema = @Schema(implementation = LivroDetalheDTO.class))),
            @ApiResponse(responseCode = "404", description = "Nenhum livro encontrado para o ID fornecido")
    })
    public ResponseEntity<LivroDetalheDTO> buscarPorId(@PathVariable Long id) {
        return livroService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'BIBLIOTECARIO', 'ALUNO')")
    @GetMapping("/catalogo-mobile")
    @Operation(summary = "Busca o catálogo de livros agrupados por gênero para o app mobile")
    public ResponseEntity<List<GeneroCatalogoDTO>> buscarCatalogoMobile() {
        List<GeneroCatalogoDTO> catalogo = livroService.buscarCatalogoParaMobile();
        return catalogo.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(catalogo);
    }

    @GetMapping("/genero/{nomeGenero}")
    @Operation(summary = "Busca livros por nome do gênero com paginação")
    @ApiResponse(responseCode = "200", description = "Página de livros retornada", content = @Content(schema = @Schema(implementation = Page.class)))
    public ResponseEntity<Page<LivroResponseMobileGeneroDTO>> buscarPorGenero(
            @PathVariable String nomeGenero, Pageable pageable) {
        Page<LivroResponseMobileGeneroDTO> livros = livroService.buscarPorGenero(nomeGenero, pageable);
        return livros.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(livros);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping(value = "/cadastrar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cadastra um novo livro, opcionalmente com a capa")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Livro cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos")
    })
    public ResponseEntity<ResponseModel> cadastrarLivro(
            @Parameter(description = "Dados do livro em formato JSON") @RequestPart("livro") LivroDTO livroDTO,
            @Parameter(description = "Arquivo de imagem da capa (opcional)") @RequestPart(value = "file", required = false) MultipartFile file) {
        return livroService.cadastrar(livroDTO, file);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping(value = "/{id}/capa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Faz upload ou atualiza a capa para um livro existente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Capa atualizada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Livro não encontrado")
    })
    public ResponseEntity<ResponseModel> uploadCapa(
            @Parameter(description = "ID do livro") @PathVariable Long id,
            @Parameter(description = "Arquivo de imagem da capa") @RequestParam("file") MultipartFile file) {
        return livroService.uploadCapa(id, file);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Atualiza um livro existente, opcionalmente com uma nova capa")
    public ResponseEntity<ResponseModel> atualizar(
            @Parameter(description = "ID do livro a ser atualizado") @PathVariable Long id,
            @Parameter(description = "Dados do livro em formato JSON") @RequestPart("livro") LivroDTO livroDTO,
            @Parameter(description = "Novo arquivo de imagem da capa (opcional)") @RequestPart(value = "file", required = false) MultipartFile file) {
        return livroService.atualizar(id, livroDTO, file);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @DeleteMapping("/{id}/com-exemplares")
    @Operation(summary = "Exclui um livro e todos os seus exemplares associados")
    public ResponseEntity<ResponseModel> excluirComExemplares(
            @Parameter(description = "ID do livro a ser excluído") @PathVariable Long id) {
        return livroService.excluirLivroComExemplares(id);
    }
}