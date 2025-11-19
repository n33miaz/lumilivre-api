package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.dto.TccResponseDTO;
import br.com.lumilivre.api.service.TccService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/tcc")
@Tag(name = "16. TCC", description = "Gerenciamento de Trabalhos de Conclusão de Curso")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'BIBLIOTECARIO')")
public class TccController {

    @Autowired
    private TccService tccService;

    @PostMapping(value = "/cadastrar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cadastra um novo TCC", description = "Recebe os dados do TCC em formato JSON (string) e um arquivo PDF opcional.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "TCC cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou arquivo incorreto", content = @Content(schema = @Schema(implementation = br.com.lumilivre.api.dto.comum.ApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    public ResponseEntity<br.com.lumilivre.api.dto.comum.ApiResponse<TccResponseDTO>> cadastrarTcc(
            @Parameter(description = "JSON contendo os dados do TCC (titulo, alunos, curso_id, etc)", required = true) @RequestParam("dadosJson") String dadosJson,

            @Parameter(description = "Arquivo PDF do TCC (Opcional)") @RequestParam(value = "arquivoPdf", required = false) MultipartFile arquivoPdf) {

        return tccService.cadastrarTcc(dadosJson, arquivoPdf);
    }

    @GetMapping("/buscar")
    @Operation(summary = "Lista todos os TCCs cadastrados")
    @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    public ResponseEntity<br.com.lumilivre.api.dto.comum.ApiResponse<List<TccResponseDTO>>> listarTccs() {
        return tccService.listarTccs();
    }

    @GetMapping("/buscar/{id}")
    @Operation(summary = "Busca um TCC pelo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "TCC encontrado"),
            @ApiResponse(responseCode = "404", description = "TCC não encontrado")
    })
    public ResponseEntity<br.com.lumilivre.api.dto.comum.ApiResponse<TccResponseDTO>> buscarPorId(@PathVariable Long id) {
        return tccService.buscarPorId(id);
    }

    @DeleteMapping("/excluir/{id}")
    @Operation(summary = "Exclui um TCC pelo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "TCC excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "TCC não encontrado")
    })
    public ResponseEntity<br.com.lumilivre.api.dto.comum.ApiResponse<Void>> excluirTcc(@PathVariable Long id) {
        return tccService.excluirTcc(id);
    }
}