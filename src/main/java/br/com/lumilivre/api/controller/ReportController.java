package br.com.lumilivre.api.controller;

import java.time.LocalDate;
import java.util.Locale;

import br.com.lumilivre.api.config.SwaggerTags;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.enums.PenaltyCode;
import br.com.lumilivre.api.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = SwaggerTags.REPORTS)
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/loans")
    @Operation(operationId = "reports.loans")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public void loans(
            HttpServletResponse response,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(required = false) String readerRegistrationNumber,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) String isbnOrCopyCode,
            @RequestParam(required = false) Integer academicModuleId,
            Locale locale) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=loans-report.pdf");
        reportService.gerarRelatorioEmprestimosPorFiltros(
                response.getOutputStream(), startDate, endDate, status,
                readerRegistrationNumber, courseId, isbnOrCopyCode, academicModuleId, locale);
    }

    @GetMapping("/readers")
    @Operation(operationId = "reports.readers")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public void readers(
            HttpServletResponse response,
            @RequestParam(required = false) Integer academicModuleId,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) Integer studyShiftId,
            @RequestParam(required = false) PenaltyCode penaltyCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Locale locale) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=readers-report.pdf");
        reportService.gerarRelatorioLeitoresPorFiltros(
                response.getOutputStream(), academicModuleId, courseId, studyShiftId,
                penaltyCode, startDate, endDate, locale);
    }

    @GetMapping("/books")
    @Operation(operationId = "reports.books")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public void books(
            HttpServletResponse response,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) String deweyCode,
            @RequestParam(required = false) String ageRating,
            @RequestParam(required = false) String coverType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Locale locale) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=books-report.pdf");
        reportService.gerarRelatorioLivrosFiltrados(
                response.getOutputStream(), genre, author, publisher, deweyCode, ageRating, coverType,
                startDate, endDate, locale);
    }

    @GetMapping("/books/statistics")
    @Operation(operationId = "reports.bookStatistics")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public void booksStatistics(HttpServletResponse response, Locale locale) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=books-statistics-report.pdf");
        reportService.gerarRelatorioEstatisticasLivros(response.getOutputStream(), locale);
    }

    @GetMapping("/copies")
    @Operation(operationId = "reports.copies")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public void copies(
            HttpServletResponse response,
            @RequestParam(required = false) BookCopyStatus status,
            @RequestParam(required = false) String isbnOrCopyCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Locale locale) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=copies-report.pdf");
        reportService.gerarRelatorioExemplaresFiltrados(
                response.getOutputStream(), status, isbnOrCopyCode, startDate, endDate, locale);
    }

    @GetMapping("/courses")
    @Operation(operationId = "reports.courses")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public void courses(HttpServletResponse response, Locale locale) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=courses-report.pdf");
        reportService.gerarRelatorioCursosGeral(response.getOutputStream(), locale);
    }
}
