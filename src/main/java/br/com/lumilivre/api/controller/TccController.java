package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.model.TccModel;
import br.com.lumilivre.api.repository.TccRepository;
import br.com.lumilivre.api.service.SupabaseStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tccs")
public class TccController {

    @Autowired
    private TccRepository tccRepository;

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> cadastrarTcc(
            @RequestPart("dados") TccModel  tcc,
            @RequestPart(value = "arquivo", required = false) MultipartFile arquivoPdf
    ) {
        try {
            if (arquivoPdf != null && !arquivoPdf.isEmpty()) {
                String urlPdf = supabaseStorageService.uploadFile(arquivoPdf, "tccs");
                tcc.setArquivo_pdf(urlPdf);
            }

            TccModel novoTcc = tccRepository.save(tcc);
            return ResponseEntity.ok(novoTcc);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao cadastrar TCC: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> listarTccs() {
        return ResponseEntity.ok(tccRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        return tccRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluirTcc(@PathVariable Long id) {
        if (!tccRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        tccRepository.deleteById(id);
        return ResponseEntity.ok("TCC excluído com sucesso.");
    }
}
