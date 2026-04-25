package br.com.lumilivre.api.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import br.com.lumilivre.api.dto.comum.ApiResponse;
import br.com.lumilivre.api.dto.genero.GeneroCatalogoResponse;
import br.com.lumilivre.api.dto.livro.LivroAgrupadoResponse;
import br.com.lumilivre.api.dto.livro.LivroDetalheResponse;
import br.com.lumilivre.api.dto.livro.LivroListagemResponse;
import br.com.lumilivre.api.dto.livro.LivroMobileResponse;
import br.com.lumilivre.api.dto.livro.LivroRequest;
import br.com.lumilivre.api.dto.livro.LivroResponse;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.security.CanAccessStudent;
import br.com.lumilivre.api.service.BookService;
import br.com.lumilivre.api.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/livros")
@Tag(name = "4. Livros")
@SecurityRequirement(name = "bearerAuth")
public class BookController {

    private final BookService bookService;
    private final RecommendationService recommendationService;

    public BookController(BookService bookService, RecommendationService recommendationService) {
        this.bookService = bookService;
        this.recommendationService = recommendationService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @GetMapping("/home")
    @Operation(summary = "Lista livros para a tela principal do admin (visão de exemplares)")
    public ResponseEntity<Page<LivroListagemResponse>> listarParaAdmin(Pageable pageable) {
        Page<LivroListagemResponse> livros = bookService.buscarParaListaAdmin(pageable);
        return livros.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(livros);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @GetMapping("/home/agrupado")
    @Operation(summary = "Lista livros agrupados por título com contagem de exemplares e busca")
    public ResponseEntity<Page<LivroAgrupadoResponse>> listarAgrupadoParaAdmin(
            @Parameter(description = "Texto para busca por nome ou ISBN") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<LivroAgrupadoResponse> livros = bookService.buscarLivrosAgrupados(pageable, texto);
        return livros.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(livros);
    }

    @GetMapping("/mobile/buscar")
    @Operation(summary = "Busca livros por título ou autor para o mobile")
    public ResponseEntity<Page<LivroMobileResponse>> buscarMobile(@RequestParam String texto, Pageable pageable) {
        Page<LivroMobileResponse> livros = bookService.buscarMobilePorTexto(texto, pageable);
        return livros.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(livros);
    }

    @GetMapping("/buscar/avancado")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "Busca avançada e paginada de livros (Agrupado)")
    public ResponseEntity<Page<LivroAgrupadoResponse>> buscarAvancado(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String autor,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) String editora,
            @RequestParam(required = false) String cdd,
            @RequestParam(required = false) String classificacaoEtaria,
            @RequestParam(required = false) String tipoCapa,
            @RequestParam(required = false) LocalDate dataLancamento,
            Pageable pageable) {

        Page<LivroAgrupadoResponse> livros = bookService.buscarAvancado(
                nome, isbn, autor, genero, editora, cdd, classificacaoEtaria, tipoCapa, dataLancamento, pageable);

        return livros.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(livros);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @GetMapping("/consulta-isbn/{isbn}")
    @Operation(summary = "Consulta dados de um livro em APIs externas (Google/BrasilAPI) para preenchimento automático")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Dados encontrados", content = @Content(schema = @Schema(implementation = LivroRequest.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Livro não encontrado nas bases externas")
    })
    public ResponseEntity<ApiResponse<LivroRequest>> consultarPorIsbn(@PathVariable String isbn) {
        try {
            LivroRequest dados = bookService.pesquisarDadosPorIsbn(isbn);
            return ResponseEntity.ok(new ApiResponse<>(true, "Dados encontrados", dados));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    @GetMapping("/{id}")
    @Operation(summary = "Busca os detalhes de um livro específico pelo seu ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Livro encontrado", content = @Content(schema = @Schema(implementation = LivroDetalheResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Nenhum livro encontrado para o ID fornecido")
    })
    public ResponseEntity<LivroDetalheResponse> buscarPorId(@PathVariable UUID id) {
        return bookService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/catalogo-mobile")
    @Operation(summary = "Busca o catálogo de livros agrupados por gênero para o app mobile")
    public ResponseEntity<List<GeneroCatalogoResponse>> buscarCatalogoMobile() {
        List<GeneroCatalogoResponse> catalogo = bookService.buscarCatalogoParaMobile();
        return catalogo.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(catalogo);
    }

    @GetMapping("/genero/{nomeGenero}")
    @Operation(summary = "Busca livros por nome do gênero com paginação")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Página de livros retornada", content = @Content(schema = @Schema(implementation = Page.class)))
    public ResponseEntity<Page<LivroMobileResponse>> buscarPorGenero(@PathVariable String nomeGenero, Pageable pageable) {
        Page<LivroMobileResponse> livros = bookService.buscarPorGenero(nomeGenero, pageable);
        return livros.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(livros);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @PostMapping(value = "/cadastrar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cadastra um novo livro, opcionalmente com a capa")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Livro cadastrado com sucesso", content = @Content(schema = @Schema(implementation = LivroResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados de entrada inválidos")
    })
    public ResponseEntity<LivroResponse> cadastrarLivro(
            @Parameter(description = "Dados do livro em formato JSON") @RequestPart("livro") LivroRequest livroDTO,
            @Parameter(description = "Arquivo de imagem da capa (opcional)") @RequestPart(value = "file", required = false) MultipartFile file) {

        LivroResponse novoLivro = bookService.cadastrar(livroDTO, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoLivro);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @PostMapping(value = "/{id}/capa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Faz upload ou atualiza a capa para um livro existente")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Capa atualizada com sucesso"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Livro não encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> uploadCapa(
            @Parameter(description = "ID do livro") @PathVariable UUID id,
            @Parameter(description = "Arquivo de imagem da capa") @RequestParam("file") MultipartFile file) {

        bookService.uploadCapa(id, file);
        return ResponseEntity.ok(new ApiResponse<>(true, "Capa atualizada com sucesso.", null));
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Atualiza um livro existente, opcionalmente com uma nova capa")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Livro atualizado com sucesso", content = @Content(schema = @Schema(implementation = LivroResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Livro não encontrado")
    })
    public ResponseEntity<LivroResponse> atualizar(
            @Parameter(description = "ID do livro a ser atualizado") @PathVariable UUID id,
            @Parameter(description = "Dados do livro em formato JSON") @RequestPart("livro") LivroRequest livroDTO,
            @Parameter(description = "Novo arquivo de imagem da capa (opcional)") @RequestPart(value = "file", required = false) MultipartFile file) {

        LivroResponse livroAtualizado = bookService.atualizar(id, livroDTO, file);
        return ResponseEntity.ok(livroAtualizado);
    }

    @CanAccessStudent
    @GetMapping("/mobile/recomendacoes/{matricula}")
    @Operation(summary = "Retorna até 10 livros recomendados com base no histórico do aluno")
    public ResponseEntity<List<LivroMobileResponse>> recomendacoes(@PathVariable String matricula) {
        return ResponseEntity.ok(recommendationService.recommendForStudent(matricula));
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @DeleteMapping("/{id}/com-exemplares")
    @Operation(summary = "Exclui um livro e todos os seus exemplares associados")
    public ResponseEntity<ApiResponse<Void>> excluirComExemplares(
            @Parameter(description = "ID do livro a ser excluído") @PathVariable UUID id) {

        bookService.excluirLivroComExemplares(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Livro e todos os exemplares foram removidos com sucesso.", null));
    }
}
