package br.com.lumilivre.api.controller;

import java.util.List;
import br.com.lumilivre.api.dto.tcc.TccResponse;
import br.com.lumilivre.api.service.TccService;
import io.swagger.v3.oas.annotations.Operation;
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
    public ResponseEntity<br.com.lumilivre.api.dto.comum.ApiResponse<TccResponse>> cadastrarTcc(
            @RequestParam("dadosJson") String dadosJson,
            @RequestParam(value = "arquivoPdf", required = false) MultipartFile arquivoPdf,
            @RequestParam(value = "arquivoFoto", required = false) MultipartFile arquivoFoto) {

        return tccService.cadastrarTcc(dadosJson, arquivoPdf, arquivoFoto);
    }

    @GetMapping("/buscar")
    @Operation(summary = "Lista TCCs (com filtro opcional)")
    public ResponseEntity<br.com.lumilivre.api.dto.comum.ApiResponse<List<TccResponse>>> listarTccs(
            @RequestParam(required = false) String texto) {
        return tccService.listarTccs(texto);
    }

    @GetMapping("/buscar/{id}")
    @Operation(summary = "Busca um TCC pelo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "TCC encontrado"),
            @ApiResponse(responseCode = "404", description = "TCC não encontrado")
    })
    public ResponseEntity<br.com.lumilivre.api.dto.comum.ApiResponse<TccResponse>> buscarPorId(@PathVariable Long id) {
        return tccService.buscarPorId(id);
    }

    @GetMapping("/buscar/avancado")
    @Operation(summary = "Busca avançada de TCCs")
    public ResponseEntity<br.com.lumilivre.api.dto.comum.ApiResponse<List<TccResponse>>> buscarAvancado(
            @RequestParam(required = false) Integer cursoId,
            @RequestParam(required = false) String semestre,
            @RequestParam(required = false) String ano) {
        return tccService.listarTccsAvancado(cursoId, semestre, ano);
    }

    @PutMapping(value = "/atualizar/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Atualiza um TCC existente")
    public ResponseEntity<br.com.lumilivre.api.dto.comum.ApiResponse<TccResponse>> atualizarTcc(
            @PathVariable Long id,
            @RequestParam("dadosJson") String dadosJson,
            @RequestParam(value = "arquivoPdf", required = false) MultipartFile arquivoPdf,
            @RequestParam(value = "arquivoFoto", required = false) MultipartFile arquivoFoto) { // Novo param
        return tccService.atualizarTcc(id, dadosJson, arquivoPdf, arquivoFoto);
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