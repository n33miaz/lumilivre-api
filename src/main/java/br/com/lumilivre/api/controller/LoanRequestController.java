package br.com.lumilivre.api.controller;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import br.com.lumilivre.api.config.SwaggerTags;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.dto.loanrequest.LoanRequestResponse;
import br.com.lumilivre.api.mapper.LoanRequestMapper;
import br.com.lumilivre.api.security.CanAccessReader;
import br.com.lumilivre.api.service.LoanRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loan-requests")
@RequiredArgsConstructor
@Tag(name = SwaggerTags.LOAN_REQUESTS)
public class LoanRequestController {

    private final LoanRequestService loanRequestService;
    private final LoanRequestMapper mapper;
    private final MessageResolver messages;

    @GetMapping
    @Operation(operationId = "loan-requests.list")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<LoanRequestResponse>> listAll(Locale locale) {
        List<LoanRequestResponse> body = loanRequestService.listAll()
                .stream()
                .map(request -> mapper.toResponse(request, locale))
                .toList();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @GetMapping("/pending")
    @Operation(operationId = "loan-requests.pending")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<LoanRequestResponse>> listPending(Locale locale) {
        List<LoanRequestResponse> body = loanRequestService.listPending()
                .stream()
                .map(request -> mapper.toResponse(request, locale))
                .toList();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @GetMapping("/reader/{registrationNumber}")
    @Operation(operationId = "loan-requests.byReader")
    @CanAccessReader
    public ResponseEntity<List<LoanRequestResponse>> listByReader(
            @PathVariable String registrationNumber,
            Locale locale) {
        List<LoanRequestResponse> body = loanRequestService.listByReader(registrationNumber)
                .stream()
                .map(request -> mapper.toResponse(request, locale))
                .toList();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PostMapping
    @Operation(operationId = "loan-requests.create")
    // Leitor só solicita em nome próprio; ADMIN/BIBLIOTECARIO liberados.
    @PreAuthorize("@readerAuthz.canAccess(#readerRegistrationNumber)")
    public ResponseEntity<String> create(
            @RequestParam String readerRegistrationNumber,
            @RequestParam String copyCode,
            Locale locale) {
        String messageKey = loanRequestService.solicitarEmprestimo(readerRegistrationNumber, copyCode);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(messages.resolve(messageKey, locale));
    }

    @PostMapping("/by-book")
    @Operation(operationId = "loan-requests.createByBook")
    // Leitor só solicita em nome próprio; ADMIN/BIBLIOTECARIO liberados.
    @PreAuthorize("@readerAuthz.canAccess(#readerRegistrationNumber)")
    public ResponseEntity<String> createByBook(
            @RequestParam String readerRegistrationNumber,
            @RequestParam UUID bookId,
            Locale locale) {
        String messageKey = loanRequestService.solicitarEmprestimoPorLivro(readerRegistrationNumber, bookId);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(messages.resolve(messageKey, locale));
    }

    @PostMapping("/{id}/process")
    @Operation(operationId = "loan-requests.process")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<String> process(
            @PathVariable UUID id,
            @RequestParam boolean accept,
            Locale locale) {
        String messageKey = loanRequestService.processarSolicitacao(id, accept);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(messages.resolve(messageKey, locale));
    }
}
