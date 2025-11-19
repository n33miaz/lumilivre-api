package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import br.com.lumilivre.api.dto.AlterarSenhaDTO;
import br.com.lumilivre.api.dto.ListaUsuarioDTO;
import br.com.lumilivre.api.dto.UsuarioDTO;
import br.com.lumilivre.api.dto.responses.UsuarioResponseDTO;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.service.UsuarioService;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

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
    @ApiResponse(responseCode = "200", description = "Página de usuários retornada com sucesso")
    public ResponseEntity<Page<ListaUsuarioDTO>> buscarUsuariosAdmin(
            @Parameter(description = "Texto para busca genérica") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<ListaUsuarioDTO> usuarios = us.buscarUsuarioParaListaAdmin(pageable);
        return usuarios.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(usuarios);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar")
    @Operation(summary = "Busca usuários com paginação e filtro de texto")
    @ApiResponse(responseCode = "200", description = "Página de usuários retornada com sucesso", content = @Content(schema = @Schema(implementation = ListaUsuarioDTO.class)))
    public ResponseEntity<Page<ListaUsuarioDTO>> buscarPorTexto(
            @Parameter(description = "Texto para busca genérica no e-mail ou matrícula") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<ListaUsuarioDTO> usuarios = us.buscarPorTexto(texto, pageable);
        return usuarios.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(usuarios);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/avancado")
    @Operation(summary = "Busca avançada e paginada de usuários")
    @ApiResponse(responseCode = "200", description = "Página de usuários retornada com sucesso", content = @Content(schema = @Schema(implementation = ListaUsuarioDTO.class)))
    public ResponseEntity<Page<ListaUsuarioDTO>> buscarAvancado(
            @Parameter(description = "ID exato do usuário") @RequestParam(required = false) Integer id,
            @Parameter(description = "E-mail parcial do usuário") @RequestParam(required = false) String email,
            @Parameter(description = "Perfil do usuário (ADMIN, BIBLIOTECARIO, ALUNO)") @RequestParam(required = false) Role role,
            Pageable pageable) {
        Page<ListaUsuarioDTO> usuarios = us.buscarAvancado(id, email, role, pageable);
        return usuarios.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(usuarios);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/cadastrar")
    @Operation(summary = "Cadastra um novo usuário (Acesso: ADMIN)", description = "Cria um novo usuário com perfil de ADMIN ou BIBLIOTECARIO.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada"),
            @ApiResponse(responseCode = "409", description = "E-mail já está em uso")
    })
    public ResponseEntity<UsuarioResponseDTO> cadastrar(@RequestBody @Valid UsuarioDTO dto) {
        UsuarioResponseDTO novoUsuario = us.cadastrarAdmin(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoUsuario);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/atualizar/{id}")
    @Operation(summary = "Atualiza um usuário existente (Acesso: ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<UsuarioResponseDTO> atualizar(
            @Parameter(description = "ID do usuário a ser atualizado") @PathVariable Integer id,
            @RequestBody @Valid UsuarioDTO dto) {
        UsuarioResponseDTO usuarioAtualizado = us.atualizar(id, dto);
        return ResponseEntity.ok(usuarioAtualizado);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/excluir/{id}")
    @Operation(summary = "Exclui um usuário (Acesso: ADMIN)")
    @ApiResponse(responseCode = "200", description = "Usuário excluído com sucesso")
    public ResponseEntity<ResponseModel> excluir(
            @Parameter(description = "ID do usuário a ser excluído") @PathVariable Integer id) {
        us.excluir(id);
        return ResponseEntity.ok(new ResponseModel("Usuário removido com sucesso"));
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/alterar-senha")
    @Operation(summary = "Altera a própria senha")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha alterada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Senha atual incorreta"),
            @ApiResponse(responseCode = "403", description = "Não autorizado a alterar senha de outro usuário"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })
    public ResponseEntity<ResponseModel> alterarSenha(@RequestBody AlterarSenhaDTO dto) {
        us.alterarSenha(dto);
        return ResponseEntity.ok(new ResponseModel("Senha alterada com sucesso"));
    }
}