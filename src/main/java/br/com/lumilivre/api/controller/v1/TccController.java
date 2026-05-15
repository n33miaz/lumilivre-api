package br.com.lumilivre.api.controller.v1;

import java.util.List;
import java.util.UUID;
import br.com.lumilivre.api.dto.v1.tcc.ThesisResponse;
import br.com.lumilivre.api.service.ThesisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tcc")
@Tag(name = "6. TCC")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
@RequiredArgsConstructor
public class TccController {

    private final ThesisService thesisService;

    @PostMapping(value = "/cadastrar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cadastra um novo TCC", description = "Recebe os dados do TCC em formato JSON (string) e um arquivo PDF opcional.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "TCC cadastrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou arquivo incorreto", content = @Content(schema = @Schema(implementation = br.com.lumilivre.api.dto.v1.comum.ApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    public ResponseEntity<br.com.lumilivre.api.dto.v1.comum.ApiResponse<ThesisResponse>> cadastrarTcc(
            @RequestParam("dadosJson") String dadosJson,
            @RequestParam(value = "arquivoPdf", required = false) MultipartFile arquivoPdf,
            @RequestParam(value = "arquivoFoto", required = false) MultipartFile arquivoFoto) {

        return thesisService.createThesis(dadosJson, arquivoPdf, arquivoFoto);
    }

    @GetMapping("/buscar")
    @Operation(summary = "Lista TCCs (com filtro opcional)")
    public ResponseEntity<br.com.lumilivre.api.dto.v1.comum.ApiResponse<List<ThesisResponse>>> listarTccs(
            @RequestParam(required = false) String texto) {
        return thesisService.listTheses(texto);
    }

    @GetMapping("/buscar/{id}")
    @Operation(summary = "Busca um TCC pelo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "TCC encontrado"),
            @ApiResponse(responseCode = "404", description = "TCC não encontrado")
    })
    public ResponseEntity<br.com.lumilivre.api.dto.v1.comum.ApiResponse<ThesisResponse>> buscarPorId(@PathVariable UUID id) {
        return thesisService.getThesisById(id);
    }

    @GetMapping("/buscar/avancado")
    @Operation(summary = "Busca avançada de TCCs")
    public ResponseEntity<br.com.lumilivre.api.dto.v1.comum.ApiResponse<List<ThesisResponse>>> buscarAvancado(
            @RequestParam(required = false) Integer cursoId,
            @RequestParam(required = false) String semestre,
            @RequestParam(required = false) String ano) {
        return thesisService.searchTheses(cursoId, semestre, ano);
    }

    @PutMapping(value = "/atualizar/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Atualiza um TCC existente")
    public ResponseEntity<br.com.lumilivre.api.dto.v1.comum.ApiResponse<ThesisResponse>> atualizarTcc(
            @PathVariable UUID id,
            @RequestParam("dadosJson") String dadosJson,
            @RequestParam(value = "arquivoPdf", required = false) MultipartFile arquivoPdf,
            @RequestParam(value = "arquivoFoto", required = false) MultipartFile arquivoFoto) {
        return thesisService.updateThesis(id, dadosJson, arquivoPdf, arquivoFoto);
    }

    @DeleteMapping("/excluir/{id}")
    @Operation(summary = "Exclui um TCC pelo ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "TCC excluído com sucesso"),
            @ApiResponse(responseCode = "404", description = "TCC não encontrado")
    })
    public ResponseEntity<br.com.lumilivre.api.dto.v1.comum.ApiResponse<Void>> excluirTcc(@PathVariable UUID id) {
        return thesisService.deleteThesis(id);
    }
}
