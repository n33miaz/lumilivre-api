package br.com.lumilivre.api.controller;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import br.com.lumilivre.api.config.SwaggerTags;
import br.com.lumilivre.api.dto.loan.ActiveLoanResponse;
import br.com.lumilivre.api.dto.loan.LoanRequest;
import br.com.lumilivre.api.dto.loan.LoanResponse;
import br.com.lumilivre.api.dto.loan.LoanStatusSummaryResponse;
import br.com.lumilivre.api.mapper.LoanMapper;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.security.CanAccessLoan;
import br.com.lumilivre.api.security.CanAccessReader;
import br.com.lumilivre.api.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = SwaggerTags.LOANS)
public class LoanController {

    private final LoanService loanService;
    private final LoanMapper mapper;

    @GetMapping
    @Operation(operationId = "loans.list")
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
    @Operation(operationId = "loans.advanced")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Page<LoanResponse>> advanced(
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(required = false) String copyCode,
            @RequestParam(required = false) String bookTitle,
            @RequestParam(required = false) String readerName,
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
                        readerName,
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
    @Operation(operationId = "loans.activeAndOverdue")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<ActiveLoanResponse>> activeAndOverdue(Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(loanService.buscarAtivosEAtrasadosV2().stream()
                        .map(item -> mapper.toActiveResponse(item, locale))
                        .toList());
    }

    @GetMapping("/overdue")
    @Operation(operationId = "loans.overdue")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<List<ActiveLoanResponse>> overdueOnly(Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(loanService.buscarApenasAtrasadosV2().stream()
                        .map(item -> mapper.toActiveResponse(item, locale))
                        .toList());
    }

    @GetMapping("/active-and-overdue/count")
    @Operation(operationId = "loans.activeAndOverdueCount")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Long> activeAndOverdueCount(Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(loanService.getContagemEmprestimosAtivosEAtrasados());
    }

    @GetMapping("/status-summary")
    @Operation(operationId = "loans.statusSummary")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<LoanStatusSummaryResponse> statusSummary(Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(loanService.getStatusSummary());
    }

    @GetMapping("/reader/{registrationNumber}")
    @Operation(operationId = "loans.byReader")
    @CanAccessReader
    public ResponseEntity<List<LoanResponse>> listByReader(
            @PathVariable String registrationNumber,
            Locale locale) {
        List<LoanResponse> body = loanService
                .listarEmprestimosLeitorV2(registrationNumber)
                .stream()
                .map(loan -> mapper.toResponse(loan, locale))
                .toList();
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @GetMapping("/reader/{registrationNumber}/history")
    @Operation(operationId = "loans.history")
    @CanAccessReader
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
    @Operation(operationId = "loans.create")
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
    @Operation(operationId = "loans.update")
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
    @Operation(operationId = "loans.close")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<LoanResponse> close(@PathVariable UUID id, Locale locale) {
        loanService.concluirEmprestimo(id);
        LoanResponse body = mapper.toResponse(loanService.buscarPorId(id), locale);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PutMapping("/{id}/renew")
    @Operation(operationId = "loans.renew")
    @CanAccessLoan
    public ResponseEntity<LoanResponse> renew(@PathVariable UUID id, Locale locale) {
        loanService.renovar(id);
        LoanResponse body = mapper.toResponse(loanService.buscarPorId(id), locale);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "loans.delete")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        loanService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
