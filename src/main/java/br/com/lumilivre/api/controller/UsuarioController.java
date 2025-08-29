package br.com.lumilivre.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.data.AlterarSenhaDTO;
import br.com.lumilivre.api.data.ListaUsuarioDTO;
import br.com.lumilivre.api.data.UsuarioDTO;
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

@Tag(name = "11. Usuários")
@SecurityRequirement(name = "bearerAuth")

public class UsuarioController {

    @Autowired
    private UsuarioService us;
    
    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/home")
    public ResponseEntity<Page<ListaUsuarioDTO>> buscarUsuariosAdmin(
            @RequestParam(required = false) String texto,
            Pageable pageable) {

        Page<ListaUsuarioDTO> usuarios = us.buscarUsuarioParaListaAdmin(pageable);

        if (usuarios.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(usuarios);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar")
    public ResponseEntity<Page<UsuarioModel>> buscarPorTexto(
            @RequestParam(required = false) String texto,
            Pageable pageable) {
        Page<UsuarioModel> usuarios = us.buscarPorTexto(texto, pageable);
        if (usuarios.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(usuarios);
    }

    @PreAuthorize("hasAnyRole('ADMIN','BIBLIOTECARIO')")
    @GetMapping("/buscar/avancado")
    public ResponseEntity<Page<UsuarioModel>> buscarAvancado( 
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Role role,
            Pageable pageable) {
        Page<UsuarioModel> usuarios = us.buscarAvancado(id, email, role, pageable);
        if (usuarios.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(usuarios);
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @PostMapping("/cadastrar")

    @Operation(summary = "Cadastra um novo usuário administrador", description = "Cria um novo usuário com perfil de ADMIN no sistema. A criação de usuários do tipo ALUNO é feita automaticamente ao cadastrar um novo aluno.")
    @ApiResponses
    ({
        @ApiResponse(responseCode = "201", description = "Usuário cadastrado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioModel.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos (ex: e-mail em branco)"),
        @ApiResponse(responseCode = "409", description = "E-mail já está em uso")
    })

    public ResponseEntity<?> cadastrar(@RequestBody @Valid UsuarioDTO dto) {
        return us.cadastrarAdmin(dto);
    }

    @PutMapping("/atualizar/{id}")

    @Operation(summary = "Atualiza um usuário existente", description = "Altera os dados de um usuário (como e-mail ou senha) com base no seu ID.")
    @ApiResponses
    ({
        @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso", content = @Content(schema = @Schema(implementation = UsuarioModel.class))),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado para o ID fornecido")
    })

    public ResponseEntity<?> atualizar(
            @Parameter(description = "ID do usuário a ser atualizado") @PathVariable Integer id, 
            @RequestBody @Valid UsuarioDTO dto) {
        return us.atualizar(id, dto);
    }

    @DeleteMapping("/excluir/{id}")

    @Operation(summary = "Exclui um usuário", description = "Remove um usuário do sistema.")
    @ApiResponse(responseCode = "200", description = "Usuário excluído com sucesso")

    public ResponseEntity<ResponseModel> excluir(
            @Parameter(description = "ID do usuário a ser excluído") @PathVariable Integer id) {
        return us.excluir(id);
    }

    @PutMapping("/alterar-senha")

    @Operation(summary = "Altera a senha do usuário logado", description = "Permite que um usuário logado altere sua própria senha, fornecendo a senha atual e a nova senha. O usuário é identificado pelo token JWT.")
    @ApiResponses
    ({
        @ApiResponse(responseCode = "200", description = "Senha alterada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Senha atual incorreta"),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado (token inválido)")
    })

    public ResponseEntity<?> alterarSenha(@RequestBody AlterarSenhaDTO dto) {
        return us.alterarSenha(dto);
    }
}