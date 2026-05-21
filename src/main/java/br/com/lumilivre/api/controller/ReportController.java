package br.com.lumilivre.api.controller;

import java.time.LocalDate;

import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.enums.PenaltyCode;
import br.com.lumilivre.api.service.ReportService;
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
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/loans")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public void loans(
            HttpServletResponse response,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(required = false) String studentRegistrationNumber,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) String isbnOrCopyCode,
            @RequestParam(required = false) Integer academicModuleId) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=loans-report.pdf");
        reportService.gerarRelatorioEmprestimosPorFiltros(
                response.getOutputStream(), startDate, endDate, status,
                studentRegistrationNumber, courseId, isbnOrCopyCode, academicModuleId);
    }

    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public void students(
            HttpServletResponse response,
            @RequestParam(required = false) Integer academicModuleId,
            @RequestParam(required = false) Integer courseId,
            @RequestParam(required = false) Integer studyShiftId,
            @RequestParam(required = false) PenaltyCode penaltyCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=students-report.pdf");
        reportService.gerarRelatorioAlunosPorFiltros(
                response.getOutputStream(), academicModuleId, courseId, studyShiftId,
                penaltyCode, startDate, endDate);
    }

    @GetMapping("/books")
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=books-report.pdf");
        reportService.gerarRelatorioLivrosFiltrados(
                response.getOutputStream(), genre, author, publisher, deweyCode, ageRating, coverType, startDate, endDate);
    }

    @GetMapping("/books/statistics")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public void booksStatistics(HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=books-statistics-report.pdf");
        reportService.gerarRelatorioEstatisticasLivros(response.getOutputStream());
    }

    @GetMapping("/copies")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public void copies(
            HttpServletResponse response,
            @RequestParam(required = false) BookCopyStatus status,
            @RequestParam(required = false) String isbnOrCopyCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=copies-report.pdf");
        reportService.gerarRelatorioExemplaresFiltrados(
                response.getOutputStream(), status, isbnOrCopyCode, startDate, endDate);
    }

    @GetMapping("/courses")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public void courses(HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=courses-report.pdf");
        reportService.gerarRelatorioCursosGeral(response.getOutputStream());
    }
}
