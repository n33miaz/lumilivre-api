package br.com.lumilivre.api.controller;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import br.com.lumilivre.api.dto.loan.ActiveLoanResponse;
import br.com.lumilivre.api.dto.loan.LoanRequest;
import br.com.lumilivre.api.dto.loan.LoanResponse;
import br.com.lumilivre.api.mapper.LoanMapper;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.security.CanAccessLoan;
import br.com.lumilivre.api.security.CanAccessStudent;
import br.com.lumilivre.api.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final LoanMapper mapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Page<LoanResponse>> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable,
            Locale locale) {
        Page<LoanResponse> page = (q == null || q.isBlank()
                ? loanService.buscarEmprestimoParaListaAdminV2(pageable)
                : loanService.buscarPorTexto(q, pageable))
                .map(item -> mapper.fromListItem(item, locale));
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(page);
    }

    @GetMapping("/advanced")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Page<LoanResponse>> advanced(
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(required = false) String copyCode,
            @RequestParam(required = false) String bookTitle,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) String borrowedAt,
            @RequestParam(required = false) String dueAt,
            @RequestParam(required = false) String dueAtStart,
            @PageableDefault(size = 20) Pageable pageable,
            Locale locale) {
        Page<LoanResponse> page = loanService
                .buscarAvancadoV2(
                        status,
                        copyCode,
                        bookTitle,
                        studentName,
                        borrowedAt,
                        dueAt,
                        dueAtStart,
                        pageable)
                .map(item -> mapper.fromListItem(item, locale));
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(page);
    }

    @GetMapping("/active-and-overdue")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<ActiveLoanResponse>> activeAndOverdue(Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(loanService.buscarAtivosEAtrasadosV2().stream()
                        .map(item -> mapper.toActiveResponse(item, locale))
                        .toList());
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<ActiveLoanResponse>> overdueOnly(Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(loanService.buscarApenasAtrasadosV2().stream()
                        .map(item -> mapper.toActiveResponse(item, locale))
                        .toList());
    }

    @GetMapping("/active-and-overdue/count")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Long> activeAndOverdueCount(Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(loanService.getContagemEmprestimosAtivosEAtrasados());
    }

    @GetMapping("/student/{registrationNumber}")
    @CanAccessStudent
    public ResponseEntity<List<LoanResponse>> listByStudent(
            @PathVariable String registrationNumber,
            Locale locale) {
        List<LoanResponse> body = loanService
                .listarEmprestimosAlunoV2(registrationNumber)
                .stream()
                .map(loan -> mapper.toResponse(loan, locale))
                .toList();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @GetMapping("/student/{registrationNumber}/history")
    @CanAccessStudent
    public ResponseEntity<List<LoanResponse>> history(
            @PathVariable String registrationNumber,
            Locale locale) {
        List<LoanResponse> body = loanService
                .listarHistoricoV2(registrationNumber)
                .stream()
                .map(loan -> mapper.toResponse(loan, locale))
                .toList();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<LoanResponse> create(
            @Valid @RequestBody LoanRequest request,
            Locale locale) {
        LoanResponse body = mapper.toResponse(loanService.cadastrar(request), locale);
        return ResponseEntity.status(201)
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<LoanResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody LoanRequest request,
            Locale locale) {
        LoanResponse body = mapper.toResponse(
                loanService.atualizar(id, request), locale);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<LoanResponse> close(@PathVariable UUID id, Locale locale) {
        loanService.concluirEmprestimo(id);
        LoanResponse body = mapper.toResponse(loanService.buscarPorId(id), locale);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PutMapping("/{id}/renew")
    @CanAccessLoan
    public ResponseEntity<LoanResponse> renew(@PathVariable UUID id, Locale locale) {
        loanService.renovar(id);
        LoanResponse body = mapper.toResponse(loanService.buscarPorId(id), locale);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        loanService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
