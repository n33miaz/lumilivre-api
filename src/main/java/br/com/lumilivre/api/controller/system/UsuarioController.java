package br.com.lumilivre.api.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import br.com.lumilivre.api.dto.auth.AlterarSenhaRequest;
import br.com.lumilivre.api.dto.comum.ApiResponse;
import br.com.lumilivre.api.dto.usuario.UsuarioRequest;
import br.com.lumilivre.api.dto.usuario.UsuarioResponse;
import br.com.lumilivre.api.dto.usuario.UsuarioResumoResponse;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/usuarios")
@Tag(name = "4. Usuários")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    @Autowired
    private UsuarioService us;

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/home")
    @Operation(summary = "Lista usuários para a tela principal do admin", description = "Retorna uma lista paginada de usuários com dados resumidos.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Página de usuários retornada com sucesso")
    public ResponseEntity<Page<UsuarioResumoResponse>> buscarUsuariosAdmin(
            @Parameter(description = "Texto para busca genérica") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<UsuarioResumoResponse> usuarios = us.buscarUsuarioParaListaAdmin(pageable);
        return usuarios.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(usuarios);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar")
    @Operation(summary = "Busca usuários com paginação e filtro de texto")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Página de usuários retornada com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResumoResponse.class)))
    public ResponseEntity<Page<UsuarioResumoResponse>> buscarPorTexto(
            @Parameter(description = "Texto para busca genérica no e-mail ou matrícula") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<UsuarioResumoResponse> usuarios = us.buscarPorTexto(texto, pageable);
        return usuarios.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(usuarios);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/avancado")
    @Operation(summary = "Busca avançada e paginada de usuários")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Página de usuários retornada com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResumoResponse.class)))
    public ResponseEntity<Page<UsuarioResumoResponse>> buscarAvancado(
            @Parameter(description = "ID exato do usuário") @RequestParam(required = false) Integer id,
            @Parameter(description = "E-mail parcial do usuário") @RequestParam(required = false) String email,
            @Parameter(description = "Perfil do usuário (ADMIN, BIBLIOTECARIO, ALUNO)") @RequestParam(required = false) Role role,
            Pageable pageable) {
        Page<UsuarioResumoResponse> usuarios = us.buscarAvancado(id, email, role, pageable);
        return usuarios.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(usuarios);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastra um novo usuário (Acesso: ADMIN)", description = "Cria um novo usuário com perfil de ADMIN ou BIBLIOTECARIO.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "E-mail já está em uso")
    })
    public ResponseEntity<UsuarioResponse> cadastrar(@RequestBody @Valid UsuarioRequest dto) {
        UsuarioResponse novoUsuario = us.cadastrarAdmin(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoUsuario);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/atualizar/{id}")
    @Operation(summary = "Atualiza um usuário existente (Acesso: ADMIN)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<UsuarioResponse> atualizar(
            @Parameter(description = "ID do usuário a ser atualizado") @PathVariable Integer id,
            @RequestBody @Valid UsuarioRequest dto) {
        UsuarioResponse usuarioAtualizado = us.atualizar(id, dto);
        return ResponseEntity.ok(usuarioAtualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/excluir/{id}")
    @Operation(summary = "Exclui um usuário (Acesso: ADMIN)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Usuário excluído com sucesso")
    public ResponseEntity<ApiResponse<Void>> excluir(
            @Parameter(description = "ID do usuário a ser excluído") @PathVariable Integer id) {
        us.excluir(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Usuário removido com sucesso", null));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/alterar-senha")
    @Operation(summary = "Altera a própria senha")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Senha alterada com sucesso"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Senha atual incorreta"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Não autorizado a alterar senha de outro usuário"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> alterarSenha(@RequestBody AlterarSenhaRequest dto) {
        us.alterarSenha(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Senha alterada com sucesso", null));
    }
}