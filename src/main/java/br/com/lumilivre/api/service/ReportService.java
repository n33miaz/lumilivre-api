package br.com.lumilivre.api.service;

import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.model.*;
import br.com.lumilivre.api.repository.StudentRepository;
import br.com.lumilivre.api.repository.CourseRepository;
import br.com.lumilivre.api.repository.LoanRepository;
import br.com.lumilivre.api.repository.BookCopyRepository;
import br.com.lumilivre.api.repository.BookRepository;
import br.com.lumilivre.api.dto.course.CourseStatisticsResponse;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.enums.PenaltyCode;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final LoanRepository loanRepository;
    private final StudentRepository alunoRepository;
    private final BookRepository livroRepository;
    private final CourseRepository courseRepository;
    private final BookCopyRepository exemplarRepository;
    private final MessageResolver messages;

    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font FONT_CABECALHO_TABELA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
    private static final Font FONT_CORPO_TABELA = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Color COR_CABECALHO_TABELA = new Color(118, 32, 117);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("pt-BR");

    public ReportService(LoanRepository loanRepository, StudentRepository alunoRepository,
            BookRepository livroRepository, CourseRepository courseRepository,
            BookCopyRepository exemplarRepository, MessageResolver messages) {
        this.loanRepository = loanRepository;
        this.alunoRepository = alunoRepository;
        this.livroRepository = livroRepository;
        this.courseRepository = courseRepository;
        this.exemplarRepository = exemplarRepository;
        this.messages = messages;
    }

    // ================= RELATÓRIOS DE EMPRÉSTIMOS =================

    public void gerarRelatorioEmprestimosPorFiltros(OutputStream out, LocalDate inicio, LocalDate fim,
            LoanStatus status, String matriculaAluno, Integer idCurso,
            String isbnOuTombo, Integer idModulo, Locale locale) throws IOException {
        Locale loc = effective(locale);
        String dashFallback = messages.resolve("report.common.fallback.dash", loc);
        String naFallback = messages.resolve("report.common.fallback.na", loc);

        try (Document document = new Document(PageSize.A4.rotate())) {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, messages.resolve("report.loans.title", loc), inicio, fim, loc);

            ZoneOffset offset = OffsetDateTime.now().getOffset();
            OffsetDateTime inicioDT = (inicio != null) ? inicio.atStartOfDay().atOffset(offset) : null;
            OffsetDateTime fimDT = (fim != null) ? fim.atTime(LocalTime.MAX).atOffset(offset) : null;

            List<Loan> emprestimos = loanRepository.findForReport(
                    inicioDT,
                    fimDT,
                    status,
                    prepararFiltroLike(matriculaAluno),
                    idCurso,
                    prepararFiltroLike(isbnOuTombo),
                    idModulo);

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 1.2f, 3.5f, 3f, 2.5f, 4f, 2.5f, 2f });

            adicionarCelulaHeader(table, messages.resolve("report.loans.col.id", loc));
            adicionarCelulaHeader(table, messages.resolve("report.loans.col.student", loc));
            adicionarCelulaHeader(table, messages.resolve("report.loans.col.course", loc));
            adicionarCelulaHeader(table, messages.resolve("report.loans.col.module", loc));
            adicionarCelulaHeader(table, messages.resolve("report.loans.col.book-copy", loc));
            adicionarCelulaHeader(table, messages.resolve("report.loans.col.borrowed-at", loc));
            adicionarCelulaHeader(table, messages.resolve("report.loans.col.status", loc));

            String unknownStudent = messages.resolve("report.loans.fallback.student-unknown", loc);
            String bookNA = messages.resolve("report.loans.fallback.book-na", loc);
            String copyNA = messages.resolve("report.loans.fallback.copy-na", loc);
            String errorLabel = messages.resolve("report.loans.error.row", loc);

            for (Loan e : emprestimos) {
                try {
                    table.addCell(criarCelulaDados(e.getId().toString()));

                    String nomeAluno = Optional.ofNullable(e.getStudent())
                            .map(Student::getFullName)
                            .orElse(unknownStudent);
                    table.addCell(criarCelulaDados(nomeAluno));

                    String nomeCurso = Optional.ofNullable(e.getStudent())
                            .map(Student::getCourse)
                            .map(Course::getName)
                            .orElse(naFallback);
                    table.addCell(criarCelulaDados(nomeCurso));

                    String nomeModulo = Optional.ofNullable(e.getStudent())
                            .map(Student::getAcademicModule)
                            .map(AcademicModule::getName)
                            .orElse(dashFallback);
                    table.addCell(criarCelulaDados(nomeModulo));

                    String livroTombo = Optional.ofNullable(e.getBookCopy())
                            .map(ex -> {
                                String nomeLivro = Optional.ofNullable(ex.getBook())
                                        .map(Book::getTitle)
                                        .orElse(bookNA);
                                return nomeLivro + " (" + ex.getCopyCode() + ")";
                            })
                            .orElse(copyNA);
                    table.addCell(criarCelulaDados(livroTombo));

                    table.addCell(criarCelulaDados(formatarData(e.getBorrowedAt(), loc)));

                    String statusStr = Optional.ofNullable(e.getStatus())
                            .map(Enum::name)
                            .orElse(dashFallback);
                    table.addCell(criarCelulaDados(statusStr));

                } catch (Exception ex) {
                    log.error("Erro ao processar linha do empréstimo ID {}: {}", e.getId(), ex.getMessage());
                    for (int i = 0; i < 7; i++)
                        table.addCell(criarCelulaDados(i == 0 ? errorLabel : dashFallback));
                }
            }

            document.add(table);
            adicionarRodapeRelatorio(document, messages.resolve("report.loans.footer.total", loc, emprestimos.size()));
        } catch (Exception ex) {
            log.error("Erro fatal ao gerar relatório de empréstimos", ex);
            throw new IOException(messages.resolve("report.common.error.generate", loc), ex);
        }
    }

    // ================= RELATÓRIOS DE ALUNOS =================

    public void gerarRelatorioAlunosPorFiltros(OutputStream out, Integer idModulo, Integer idCurso,
            Integer idTurno, PenaltyCode penalidade, LocalDate dataInicio, LocalDate dataFim, Locale locale)
            throws IOException {
        Locale loc = effective(locale);
        String dashFallback = messages.resolve("report.common.fallback.dash", loc);
        String naFallback = messages.resolve("report.common.fallback.na", loc);

        try (Document document = new Document(PageSize.A4.rotate())) {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, messages.resolve("report.students.title", loc),
                    dataInicio, dataFim, loc);

            ZoneOffset offset = OffsetDateTime.now().getOffset();
            OffsetDateTime inicio = (dataInicio != null) ? dataInicio.atStartOfDay().atOffset(offset) : null;
            OffsetDateTime fim = (dataFim != null) ? dataFim.atTime(23, 59, 59).atOffset(offset) : null;

            List<Student> alunos = alunoRepository.findForReport(idModulo, idCurso, idTurno, penalidade, inicio, fim);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 2f, 5f, 3f, 2f, 2f, 2f });

            adicionarCelulaHeader(table, messages.resolve("report.students.col.registration", loc));
            adicionarCelulaHeader(table, messages.resolve("report.students.col.name", loc));
            adicionarCelulaHeader(table, messages.resolve("report.students.col.course", loc));
            adicionarCelulaHeader(table, messages.resolve("report.students.col.module", loc));
            adicionarCelulaHeader(table, messages.resolve("report.students.col.penalty", loc));
            adicionarCelulaHeader(table, messages.resolve("report.students.col.loan-count", loc));

            for (Student a : alunos) {
                long totalLoans = loanRepository.countByStudent_RegistrationNumberAndStatus(
                        a.getRegistrationNumber(), LoanStatus.ACTIVE)
                        + loanRepository.countByStudent_RegistrationNumberAndStatus(
                                a.getRegistrationNumber(), LoanStatus.COMPLETED)
                        + loanRepository.countByStudent_RegistrationNumberAndStatus(
                                a.getRegistrationNumber(), LoanStatus.OVERDUE);

                table.addCell(criarCelulaDados(a.getRegistrationNumber()));
                table.addCell(criarCelulaDados(a.getFullName()));
                table.addCell(criarCelulaDados(
                        Optional.ofNullable(a.getCourse()).map(Course::getName).orElse(naFallback)));
                table.addCell(criarCelulaDados(
                        Optional.ofNullable(a.getAcademicModule()).map(AcademicModule::getName).orElse(dashFallback)));
                table.addCell(criarCelulaDados(
                        Optional.ofNullable(a.getPenaltyCode()).map(Enum::name).orElse(dashFallback)));
                table.addCell(criarCelulaDados(String.valueOf(totalLoans)));
            }

            document.add(table);
            adicionarRodapeRelatorio(document, messages.resolve("report.students.footer.total", loc, alunos.size()));
        } catch (Exception ex) {
            log.error("Erro ao gerar relatório de alunos filtrados", ex);
            throw new IOException(messages.resolve("report.common.error.generate", loc), ex);
        }
    }

    // ================= RELATÓRIOS DE CURSOS =================

    public void gerarRelatorioCursosGeral(OutputStream out, Locale locale) throws IOException {
        Locale loc = effective(locale);

        try (Document document = new Document(PageSize.A4.rotate())) {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, messages.resolve("report.courses.title", loc), null, null, loc);

            List<CourseStatisticsResponse> estatisticas = courseRepository.findStatistics();

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 3f, 2f, 2f, 2f });

            adicionarCelulaHeader(table, messages.resolve("report.courses.col.course", loc));
            adicionarCelulaHeader(table, messages.resolve("report.courses.col.student-count", loc));
            adicionarCelulaHeader(table, messages.resolve("report.courses.col.loan-count", loc));
            adicionarCelulaHeader(table, messages.resolve("report.courses.col.avg-loans", loc));

            for (CourseStatisticsResponse dto : estatisticas) {
                table.addCell(criarCelulaDados(dto.getCourseName()));
                table.addCell(criarCelulaDados(String.valueOf(dto.getStudentCount())));
                table.addCell(criarCelulaDados(String.valueOf(dto.getTotalLoans())));
                table.addCell(criarCelulaDados(String.format("%.2f", dto.getAvgLoansPerStudent())));
            }

            document.add(table);
            adicionarRodapeRelatorio(document,
                    messages.resolve("report.courses.footer.total", loc, estatisticas.size()));
        } catch (Exception ex) {
            log.error("Erro ao gerar relatório geral de cursos", ex);
            throw new IOException(messages.resolve("report.common.error.generate", loc), ex);
        }
    }

    // ================= RELATÓRIOS DE LIVROS E EXEMPLARES =================

    public void gerarRelatorioLivrosFiltrados(OutputStream out, String genero, String autor,
            String editora, String cdd, String classificacaoEtaria, String tipoCapa,
            LocalDate dataInicio, LocalDate dataFim, Locale locale) throws IOException {
        Locale loc = effective(locale);
        String dashFallback = messages.resolve("report.common.fallback.dash", loc);

        try (Document document = new Document(PageSize.A4.rotate())) {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, messages.resolve("report.books.title", loc), dataInicio, dataFim, loc);

            ZoneOffset offset = OffsetDateTime.now().getOffset();
            OffsetDateTime inicio = (dataInicio != null) ? dataInicio.atStartOfDay().atOffset(offset) : null;
            OffsetDateTime fim = (dataFim != null) ? dataFim.atTime(23, 59, 59).atOffset(offset) : null;

            List<Book> livros = livroRepository.findForReport(
                    prepararFiltroLike(genero),
                    prepararFiltroLike(autor),
                    prepararFiltroLike(editora),
                    cdd,
                    classificacaoEtaria,
                    tipoCapa,
                    inicio,
                    fim);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 1.2f, 4f, 3f, 3f, 2f, 2f });

            adicionarCelulaHeader(table, messages.resolve("report.books.col.id", loc));
            adicionarCelulaHeader(table, messages.resolve("report.books.col.title", loc));
            adicionarCelulaHeader(table, messages.resolve("report.books.col.author", loc));
            adicionarCelulaHeader(table, messages.resolve("report.books.col.genres", loc));
            adicionarCelulaHeader(table, messages.resolve("report.books.col.dewey", loc));
            adicionarCelulaHeader(table, messages.resolve("report.books.col.copy-count", loc));

            for (Book l : livros) {
                long qtdExemplares = exemplarRepository.countByBook_Id(l.getId());
                table.addCell(criarCelulaDados(l.getId().toString()));
                table.addCell(criarCelulaDados(l.getTitle()));
                table.addCell(criarCelulaDados(l.getAuthor()));
                String generos = l.getGenres().stream().map(Genre::getName).collect(Collectors.joining(", "));
                table.addCell(criarCelulaDados(generos.isEmpty() ? dashFallback : generos));
                table.addCell(criarCelulaDados(
                        Optional.ofNullable(l.getDeweyClassification()).map(DeweyClassification::getCode)
                                .orElse(dashFallback)));
                table.addCell(criarCelulaDados(String.valueOf(qtdExemplares)));
            }

            document.add(table);
            adicionarRodapeRelatorio(document, messages.resolve("report.books.footer.total", loc, livros.size()));
        } catch (Exception ex) {
            log.error("Erro ao gerar relatório de livros filtrados", ex);
            throw new IOException(messages.resolve("report.common.error.generate", loc), ex);
        }
    }

    public void gerarRelatorioEstatisticasLivros(OutputStream out, Locale locale) throws IOException {
        Locale loc = effective(locale);

        try (Document document = new Document(PageSize.A4)) {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, messages.resolve("report.books-statistics.title", loc),
                    null, null, loc);

            long totalTitulos = livroRepository.count();
            List<Map<String, Object>> porAutor = livroRepository.countByAutor();
            List<Map<String, Object>> porGenero = livroRepository.countByGenero();

            PdfPTable tableResumo = new PdfPTable(2);
            tableResumo.setWidthPercentage(50);
            adicionarCelulaHeader(tableResumo, messages.resolve("report.books-statistics.col.metric", loc));
            adicionarCelulaHeader(tableResumo, messages.resolve("report.books-statistics.col.value", loc));
            tableResumo.addCell(criarCelulaDados(messages.resolve("report.books-statistics.metric.total-titles", loc)));
            tableResumo.addCell(criarCelulaDados(String.valueOf(totalTitulos)));
            document.add(tableResumo);
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph(messages.resolve("report.books-statistics.section.top-authors", loc),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            PdfPTable tAutor = new PdfPTable(2);
            tAutor.setWidthPercentage(80);
            tAutor.setSpacingBefore(10);
            adicionarCelulaHeader(tAutor, messages.resolve("report.books-statistics.col.author", loc));
            adicionarCelulaHeader(tAutor, messages.resolve("report.books-statistics.col.title-count", loc));
            porAutor.stream().limit(10).forEach(e -> {
                tAutor.addCell(criarCelulaDados(String.valueOf(e.get("autor"))));
                tAutor.addCell(criarCelulaDados(String.valueOf(e.get("total"))));
            });
            document.add(tAutor);
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph(messages.resolve("report.books-statistics.section.titles-by-genre", loc),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            PdfPTable tGenero = new PdfPTable(2);
            tGenero.setWidthPercentage(80);
            tGenero.setSpacingBefore(10);
            adicionarCelulaHeader(tGenero, messages.resolve("report.books-statistics.col.genre", loc));
            adicionarCelulaHeader(tGenero, messages.resolve("report.books-statistics.col.title-count", loc));
            porGenero.forEach(e -> {
                tGenero.addCell(criarCelulaDados(String.valueOf(e.get("genero"))));
                tGenero.addCell(criarCelulaDados(String.valueOf(e.get("total"))));
            });
            document.add(tGenero);

        } catch (Exception ex) {
            log.error("Erro ao gerar estatísticas de livros", ex);
            throw new IOException(messages.resolve("report.common.error.generate", loc), ex);
        }
    }

    public void gerarRelatorioExemplaresFiltrados(OutputStream out, BookCopyStatus status, String isbnOuTombo,
            LocalDate dataInicio, LocalDate dataFim, Locale locale) throws IOException {
        Locale loc = effective(locale);
        String dashFallback = messages.resolve("report.common.fallback.dash", loc);
        String naFallback = messages.resolve("report.common.fallback.na", loc);

        try (Document document = new Document(PageSize.A4.rotate())) {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, messages.resolve("report.copies.title", loc), dataInicio, dataFim,
                    loc);

            ZoneOffset offset = OffsetDateTime.now().getOffset();
            OffsetDateTime inicio = (dataInicio != null) ? dataInicio.atStartOfDay().atOffset(offset) : null;
            OffsetDateTime fim = (dataFim != null) ? dataFim.atTime(23, 59, 59).atOffset(offset) : null;

            List<BookCopy> exemplares = exemplarRepository.findForReport(
                    status,
                    prepararFiltroLike(isbnOuTombo),
                    inicio,
                    fim);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 2f, 4f, 2.5f, 3f, 3f });

            adicionarCelulaHeader(table, messages.resolve("report.copies.col.copy-code", loc));
            adicionarCelulaHeader(table, messages.resolve("report.copies.col.book-title", loc));
            adicionarCelulaHeader(table, messages.resolve("report.copies.col.status", loc));
            adicionarCelulaHeader(table, messages.resolve("report.copies.col.shelf-location", loc));
            adicionarCelulaHeader(table, messages.resolve("report.copies.col.isbn", loc));

            for (BookCopy ex : exemplares) {
                table.addCell(criarCelulaDados(ex.getCopyCode()));
                table.addCell(criarCelulaDados(
                        Optional.ofNullable(ex.getBook()).map(Book::getTitle).orElse(naFallback)));
                table.addCell(criarCelulaDados(
                        Optional.ofNullable(ex.getStatus()).map(Enum::name).orElse(dashFallback)));
                table.addCell(criarCelulaDados(Optional.ofNullable(ex.getShelfLocation()).orElse(dashFallback)));
                table.addCell(criarCelulaDados(
                        Optional.ofNullable(ex.getBook()).map(Book::getIsbn).orElse(dashFallback)));
            }

            document.add(table);
            adicionarRodapeRelatorio(document,
                    messages.resolve("report.copies.footer.total", loc, exemplares.size()));
        } catch (Exception ex) {
            log.error("Erro ao gerar relatório de exemplares filtrados", ex);
            throw new IOException(messages.resolve("report.common.error.generate", loc), ex);
        }
    }

    // ================= MÉTODOS AUXILIARES (HELPERS) =================

    private void adicionarCabecalhoRelatorio(Document document, String titulo, LocalDate inicio, LocalDate fim,
            Locale locale) throws DocumentException {
        Paragraph pTitulo = new Paragraph(titulo, FONT_TITULO);
        pTitulo.setAlignment(Element.ALIGN_CENTER);
        pTitulo.setSpacingAfter(10);
        document.add(pTitulo);

        String periodoStr = (inicio != null && fim != null)
                ? messages.resolve("report.period.range", locale,
                        inicio.format(DATE_FORMATTER), fim.format(DATE_FORMATTER))
                : messages.resolve("report.period.all", locale);
        Paragraph pPeriodo = new Paragraph(periodoStr, FontFactory.getFont(FontFactory.HELVETICA, 10));
        pPeriodo.setAlignment(Element.ALIGN_CENTER);
        pPeriodo.setSpacingAfter(20);
        document.add(pPeriodo);
    }

    private void adicionarRodapeRelatorio(Document document, String texto) throws DocumentException {
        Paragraph pTotal = new Paragraph(texto, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
        pTotal.setAlignment(Element.ALIGN_RIGHT);
        pTotal.setSpacingBefore(15);
        document.add(pTotal);
    }

    private void adicionarCelulaHeader(PdfPTable table, String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FONT_CABECALHO_TABELA));
        cell.setBackgroundColor(COR_CABECALHO_TABELA);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        cell.setBorderColor(Color.GRAY);
        table.addCell(cell);
    }

    private PdfPCell criarCelulaDados(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "", FONT_CORPO_TABELA));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setBorderColor(Color.LIGHT_GRAY);
        return cell;
    }

    private String formatarData(OffsetDateTime data, Locale locale) {
        return (data != null) ? data.format(DATE_FORMATTER) : messages.resolve("report.common.fallback.na", locale);
    }

    private String prepararFiltroLike(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        return "%" + valor.trim() + "%";
    }

    private Locale effective(Locale locale) {
        return locale != null ? locale : DEFAULT_LOCALE;
    }
}
