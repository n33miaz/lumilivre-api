package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import br.com.lumilivre.api.data.ListaLivroDTO;
import br.com.lumilivre.api.data.LivroDTO;
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
@CrossOrigin(origins = "*", maxAge = 3600, allowCredentials = "false")

@Tag(name = "6. Livros")
@SecurityRequirement(name = "bearerAuth")

public class LivroController {

    @Autowired
    private LivroService ls;

    public LivroController(LivroService livroService) {
        this.ls = livroService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/home")

    @Operation(summary = "Lista livros para a tela principal do admin", description = "Retorna uma lista paginada de livros com dados resumidos (usando ListaLivroDTO) para a exibição no dashboard do admin. Suporta filtro de texto.")
    @ApiResponse(responseCode = "200", description = "Página de livros retornada com sucesso")

    public ResponseEntity<Page<ListaLivroDTO>> listarParaAdmin(
            @Parameter(description = "Texto para busca genérica") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<ListaLivroDTO> livros = ls.buscarParaListaAdmin(pageable);

        return livros.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(livros);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @GetMapping("/{isbn}")

    @Operation(summary = "Busca um livro específico pelo ISBN", description = "Retorna os detalhes completos de um único livro.")
    @ApiResponses
    ({
        @ApiResponse(responseCode = "200", description = "Livro encontrado", content = @Content(schema = @Schema(implementation = LivroModel.class))),
        @ApiResponse(responseCode = "404", description = "Nenhum livro encontrado para o ISBN fornecido")
    })

    public ResponseEntity<LivroModel> buscarPorIsbn(
            @Parameter(description = "ISBN do livro a ser buscado") @PathVariable String isbn) {
        return ls.findByIsbn(isbn);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO', 'ALUNO')")
    @GetMapping("/buscar")

    @Operation(summary = "Busca livros com paginação e filtro de texto", description = "Endpoint de busca geral para livros, usado tanto por administradores quanto por alunos.")
    @ApiResponse(responseCode = "200", description = "Página de livros retornada com sucesso")

    public ResponseEntity<Page<LivroModel>> buscarPorTexto(
            @Parameter(description = "Texto para busca genérica (em título, sinopse, autor, etc.)") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<LivroModel> livros = ls.buscarPorTexto(texto, pageable);

        return livros.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(livros);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO', 'ALUNO')")
    @GetMapping("/buscar/avancado")

    @Operation(summary = "Busca avançada e paginada de livros", description = "Filtra livros por campos específicos como título, ISBN, autor, gênero e editora.")

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

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO','ALUNO')")
    @GetMapping("/disponiveis")

    @Operation(summary = "Lista livros com exemplares disponíveis", description = "Retorna uma lista de obras que possuem pelo menos um exemplar com status 'DISPONÍVEL'.")

    public ResponseEntity<Iterable<LivroModel>> listarDisponiveis() {
        Iterable<LivroModel> livrosDisponiveis = ls.buscarLivrosDisponiveis();

        return !livrosDisponiveis.iterator().hasNext() ? ResponseEntity.noContent().build() : ResponseEntity.ok(livrosDisponiveis);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PostMapping("/cadastrar")

    @Operation(summary = "Cadastra um novo livro", description = "Cria uma nova obra (não um exemplar) no sistema.")
    @ApiResponses
    ({
        @ApiResponse(responseCode = "201", description = "Livro cadastrado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos")
    })

    public ResponseEntity<?> cadastrar(@RequestBody LivroDTO livroDTO) {
        return ls.cadastrar(livroDTO);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @PutMapping("/atualizar/{isbn}")

    @Operation(summary = "Atualiza um livro existente", description = "Altera os dados de uma obra com base no seu ISBN.")

    public ResponseEntity<?> atualizar(
            @Parameter(description = "ISBN do livro a ser atualizado") @PathVariable String isbn, 
            @RequestBody LivroDTO livroDTO) {
        return ls.atualizar(livroDTO);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @DeleteMapping("/excluir/{isbn}")

    @Operation(summary = "Exclui um livro", description = "Remove uma obra. Só funciona se não houver exemplares associados.")

    public ResponseEntity<ResponseModel> excluir(
            @Parameter(description = "ISBN do livro a ser excluído") @PathVariable String isbn) {
        return ls.excluir(isbn);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN')")
    @DeleteMapping("/excluir-com-exemplares/{isbn}")

    @Operation(summary = "Exclui um livro e seus exemplares (Acesso: ADMIN)", description = "Operação perigosa que remove uma obra e TODOS os seus exemplares. Requer permissão de Administrador.")

    public ResponseEntity<?> excluirComExemplares(
            @Parameter(description = "ISBN do livro e exemplares a serem excluídos") @PathVariable String isbn) {
        return ls.excluirLivroComExemplares(isbn);
    }
}