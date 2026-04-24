package br.com.lumilivre.api.controller.system;

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
import br.com.lumilivre.api.service.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/usuarios")
@Tag(name = "2. UsuÃ¡rios")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserService appUserService;

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @GetMapping("/home")
    @Operation(summary = "Lista usuÃ¡rios para a tela principal do admin", description = "Retorna uma lista paginada de usuÃ¡rios com dados resumidos.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PÃ¡gina de usuÃ¡rios retornada com sucesso")
    public ResponseEntity<Page<UsuarioResumoResponse>> buscarUsuariosAdmin(
            @Parameter(description = "Texto para busca genÃ©rica") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<UsuarioResumoResponse> usuarios = appUserService.buscarUsuarioParaListaAdmin(pageable);
        return usuarios.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(usuarios);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @GetMapping("/buscar")
    @Operation(summary = "Busca usuÃ¡rios com paginaÃ§Ã£o e filtro de texto")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PÃ¡gina de usuÃ¡rios retornada com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResumoResponse.class)))
    public ResponseEntity<Page<UsuarioResumoResponse>> buscarPorTexto(
            @Parameter(description = "Texto para busca genÃ©rica no e-mail ou matrÃ­cula") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<UsuarioResumoResponse> usuarios = appUserService.buscarPorTexto(texto, pageable);
        return usuarios.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(usuarios);
    }

    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @GetMapping("/buscar/avancado")
    @Operation(summary = "Busca avanÃ§ada e paginada de usuÃ¡rios")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PÃ¡gina de usuÃ¡rios retornada com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResumoResponse.class)))
    public ResponseEntity<Page<UsuarioResumoResponse>> buscarAvancado(
            @Parameter(description = "ID exato do usuÃ¡rio") @RequestParam(required = false) Integer id,
            @Parameter(description = "E-mail parcial do usuÃ¡rio") @RequestParam(required = false) String email,
            @Parameter(description = "Perfil do usuÃ¡rio (ADMIN, BIBLIOTECARIO, ALUNO)") @RequestParam(required = false) Role role,
            Pageable pageable) {
        Page<UsuarioResumoResponse> usuarios = appUserService.buscarAvancado(id, email, role, pageable);
        return usuarios.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(usuarios);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastra um novo usuÃ¡rio (Acesso: ADMIN)", description = "Cria um novo usuÃ¡rio com perfil de ADMIN ou BIBLIOTECARIO.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "UsuÃ¡rio cadastrado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados invÃ¡lidos ou regra de negÃ³cio violada"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "E-mail jÃ¡ estÃ¡ em uso")
    })
    public ResponseEntity<UsuarioResponse> cadastrar(@RequestBody @Valid UsuarioRequest dto) {
        UsuarioResponse novoUsuario = appUserService.cadastrarAdmin(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoUsuario);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/atualizar/{id}")
    @Operation(summary = "Atualiza um usuÃ¡rio existente (Acesso: ADMIN)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "UsuÃ¡rio atualizado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "UsuÃ¡rio nÃ£o encontrado")
    })
    public ResponseEntity<UsuarioResponse> atualizar(
            @Parameter(description = "ID do usuÃ¡rio a ser atualizado") @PathVariable Integer id,
            @RequestBody @Valid UsuarioRequest dto) {
        UsuarioResponse usuarioAtualizado = appUserService.atualizar(id, dto);
        return ResponseEntity.ok(usuarioAtualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/excluir/{id}")
    @Operation(summary = "Exclui um usuÃ¡rio (Acesso: ADMIN)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "UsuÃ¡rio excluÃ­do com sucesso")
    public ResponseEntity<ApiResponse<Void>> excluir(
            @Parameter(description = "ID do usuÃ¡rio a ser excluÃ­do") @PathVariable Integer id) {
        appUserService.excluir(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "UsuÃ¡rio removido com sucesso", null));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/alterar-senha")
    @Operation(summary = "Altera a prÃ³pria senha")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Senha alterada com sucesso"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Senha atual incorreta"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "NÃ£o autorizado a alterar senha de outro usuÃ¡rio"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "UsuÃ¡rio nÃ£o encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> alterarSenha(@RequestBody AlterarSenhaRequest dto) {
        appUserService.alterarSenha(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Senha alterada com sucesso", null));
    }
}
