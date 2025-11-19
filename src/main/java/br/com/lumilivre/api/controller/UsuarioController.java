package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import br.com.lumilivre.api.dto.AlterarSenhaDTO;
import br.com.lumilivre.api.dto.ListaUsuarioDTO;
import br.com.lumilivre.api.dto.UsuarioDTO;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.model.UsuarioModel;
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

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/home")

    @Operation(summary = "Lista usuários para a tela principal do admin", description = "Retorna uma lista paginada de usuários com dados resumidos para a exibição no dashboard. Suporta filtro de texto.")
    @ApiResponse(responseCode = "200", description = "Página de usuários retornada com sucesso")

    public ResponseEntity<Page<ListaUsuarioDTO>> buscarUsuariosAdmin(
            @Parameter(description = "Texto para busca genérica") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<ListaUsuarioDTO> usuarios = us.buscarUsuarioParaListaAdmin(pageable);

        return usuarios.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(usuarios);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar")

    @Operation(summary = "Busca usuários com paginação e filtro de texto", description = "Retorna uma página de usuários com detalhes completos.")
    @ApiResponse(responseCode = "200", description = "Página de usuários retornada com sucesso")

    public ResponseEntity<Page<UsuarioModel>> buscarPorTexto(
            @Parameter(description = "Texto para busca genérica no e-mail ou matrícula") @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<UsuarioModel> usuarios = us.buscarPorTexto(texto, pageable);

        return usuarios.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(usuarios);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/avancado")

    @Operation(summary = "Busca avançada e paginada de usuários", description = "Filtra usuários por ID, e-mail ou perfil (role).")
    @ApiResponse(responseCode = "200", description = "Página de usuários retornada com sucesso")

    public ResponseEntity<Page<UsuarioModel>> buscarAvancado(
            @Parameter(description = "ID exato do usuário") @RequestParam(required = false) Integer id,
            @Parameter(description = "E-mail parcial do usuário") @RequestParam(required = false) String email,
            @Parameter(description = "Perfil do usuário (ADMIN, BIBLIOTECARIO, ALUNO)") @RequestParam(required = false) Role role,
            Pageable pageable) {
        Page<UsuarioModel> usuarios = us.buscarAvancado(id, email, role, pageable);

        return usuarios.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(usuarios);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/cadastrar")

    @Operation(summary = "Cadastra um novo usuário (Acesso: ADMIN)", description = "Cria um novo usuário com perfil de ADMIN ou BIBLIOTECARIO. A criação de usuários ALUNO é automática via cadastro de aluno.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioModel.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "E-mail já está em uso")
    })

    public ResponseEntity<?> cadastrar(@RequestBody @Valid UsuarioDTO dto) {
        return us.cadastrarAdmin(dto);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/atualizar/{id}")

    @Operation(summary = "Atualiza um usuário existente (Acesso: ADMIN)", description = "Altera os dados de um usuário (e-mail, senha, perfil) com base no seu ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioModel.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })

    public ResponseEntity<?> atualizar(
            @Parameter(description = "ID do usuário a ser atualizado") @PathVariable Integer id,
            @RequestBody @Valid UsuarioDTO dto) {
        return us.atualizar(id, dto);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/excluir/{id}")

    @Operation(summary = "Exclui um usuário (Acesso: ADMIN)", description = "Remove um usuário do sistema.")
    @ApiResponse(responseCode = "200", description = "Usuário excluído com sucesso")

    public ResponseEntity<ResponseModel> excluir(
            @Parameter(description = "ID do usuário a ser excluído") @PathVariable Integer id) {
        return us.excluir(id);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/alterar-senha")

    @Operation(summary = "Altera a própria senha", description = "Permite que um usuário logado altere sua própria senha, fornecendo a matrícula (do DTO), a senha atual e a nova senha.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha alterada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Senha atual incorreta"),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    })

    public ResponseEntity<?> alterarSenha(@RequestBody AlterarSenhaDTO dto) {
        return us.alterarSenha(dto);
    }
}