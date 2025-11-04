package br.com.lumilivre.api.controller;

import br.com.lumilivre.api.service.TccService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tccs")
public class TccController {

    @Autowired
    private TccService tccService;

    // Cadastrar novo TCC
    @PostMapping("cadastrar")
    public ResponseEntity<?> cadastrarTcc(
            @RequestParam("dadosJson") String dadosJson,
            @RequestParam(value = "arquivoPdf", required = false) MultipartFile arquivoPdf) {
        return tccService.cadastrarTcc(dadosJson, arquivoPdf);
    }

    // Listar todos os TCCs
    @GetMapping("buscar")
    public ResponseEntity<?> listarTccs() {
        return tccService.listarTccs();
    }

    // Buscar TCC por ID
    @GetMapping("buscar/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        return tccService.buscarPorId(id);
    }

    // Excluir TCC
    @DeleteMapping("excluir/{id}")
    public ResponseEntity<?> excluirTcc(@PathVariable Long id) {
        return tccService.excluirTcc(id);
    }
}
