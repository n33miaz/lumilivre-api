package br.com.lumilivre.api.controller.v1;

import java.util.List;
import br.com.lumilivre.api.dto.v1.comum.ItemSimplesResponse;
import br.com.lumilivre.api.repository.DeweyClassificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({ "/cdd", "/cdds" })
@Tag(name = "13. CDD")
@RequiredArgsConstructor
public class ClassificacaoDeweyController {

    private final DeweyClassificationRepository deweyClassificationRepository;

    @GetMapping
    @Operation(summary = "Lista todas as classificações CDD disponíveis")
    public ResponseEntity<List<ItemSimplesResponse>> listarTodos() {
        var lista = deweyClassificationRepository.findAll().stream()
                .map(cdd -> new ItemSimplesResponse(cdd.getCode(), cdd.getDescription()))
                .toList();

        return ResponseEntity.ok(lista);
    }
}
