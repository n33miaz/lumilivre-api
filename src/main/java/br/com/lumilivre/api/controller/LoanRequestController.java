package br.com.lumilivre.api.controller;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.dto.loanrequest.LoanRequestResponse;
import br.com.lumilivre.api.mapper.LoanRequestMapper;
import br.com.lumilivre.api.security.CanAccessStudent;
import br.com.lumilivre.api.service.LoanRequestService;
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
public class LoanRequestController {

    private final LoanRequestService loanRequestService;
    private final LoanRequestMapper mapper;
    private final MessageResolver messages;

    @GetMapping
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

    @GetMapping("/student/{registrationNumber}")
    @CanAccessStudent
    public ResponseEntity<List<LoanRequestResponse>> listByStudent(
            @PathVariable String registrationNumber,
            Locale locale) {
        List<LoanRequestResponse> body = loanRequestService.listByStudent(registrationNumber)
                .stream()
                .map(request -> mapper.toResponse(request, locale))
                .toList();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    public ResponseEntity<String> create(
            @RequestParam String studentRegistrationNumber,
            @RequestParam String copyCode,
            Locale locale) {
        String messageKey = loanRequestService.solicitarEmprestimo(studentRegistrationNumber, copyCode);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(messages.resolve(messageKey, locale));
    }

    @PostMapping("/by-book")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    public ResponseEntity<String> createByBook(
            @RequestParam String studentRegistrationNumber,
            @RequestParam UUID bookId,
            Locale locale) {
        String messageKey = loanRequestService.solicitarEmprestimoPorLivro(studentRegistrationNumber, bookId);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(messages.resolve(messageKey, locale));
    }

    @PostMapping("/{id}/process")
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
