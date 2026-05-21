package br.com.lumilivre.api.controller;

import java.util.List;
import java.util.Locale;

import br.com.lumilivre.api.dto.dewey.DeweyClassificationResponse;
import br.com.lumilivre.api.repository.DeweyClassificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dewey-classifications")
@RequiredArgsConstructor
public class DeweyClassificationController {

    private final DeweyClassificationRepository deweyClassificationRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    public ResponseEntity<List<DeweyClassificationResponse>> list(Locale locale) {
        List<DeweyClassificationResponse> body = deweyClassificationRepository.findAll()
                .stream()
                .map(d -> new DeweyClassificationResponse(d.getCode(), d.getDescription()))
                .toList();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }
}
