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
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates the branded, internationalized PDF reports. Every report shares a
 * consistent visual system: a brand header band (logo + wordmark + tagline),
 * a summary row of KPI tiles, color-coded status badges (enum labels resolved
 * per {@link Locale} — ADR-009), zebra-striped tables, and a per-page footer
 * (brand · timestamp · confidential · page number).
 */
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

    // ---- Brand palette (mirrors web tailwind.config.js: #5E195D / #762075 / #9D4D9C) ----
    private static final Color COR_BRAND = new Color(118, 32, 117);
    private static final Color COR_TEXTO = new Color(43, 35, 51);
    private static final Color COR_TEXTO_SEC = new Color(120, 120, 128);
    private static final Color COR_ZEBRA = new Color(249, 245, 250);
    private static final Color COR_KPI_BG = new Color(246, 241, 250);
    private static final Color COR_CELULA_BORDA = new Color(232, 226, 236);
    private static final Color COR_RODAPE_LINHA = new Color(222, 218, 226);
    private static final Color COR_BAND_TAG = new Color(231, 219, 236);

    // ---- Semantic status colors (foreground / soft background) ----
    private static final Color FG_GREEN = new Color(28, 132, 74), BG_GREEN = new Color(231, 246, 238);
    private static final Color FG_RED = new Color(185, 52, 42), BG_RED = new Color(252, 235, 234);
    private static final Color FG_AMBER = new Color(160, 110, 8), BG_AMBER = new Color(252, 244, 224);
    private static final Color FG_BLUE = new Color(42, 90, 158), BG_BLUE = new Color(232, 239, 250);
    private static final Color FG_GRAY = new Color(92, 99, 110), BG_GRAY = new Color(237, 238, 241);

    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, COR_BRAND);
    private static final Font FONT_SECAO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COR_TEXTO);
    private static final Font FONT_CABECALHO_TABELA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font FONT_CORPO_TABELA = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, COR_TEXTO);
    private static final Font FONT_META = FontFactory.getFont(FontFactory.HELVETICA, 9, COR_TEXTO_SEC);
    private static final Font FONT_RODAPE = FontFactory.getFont(FontFactory.HELVETICA, 8, COR_TEXTO_SEC);
    private static final Font FONT_BAND_BRAND = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, Color.WHITE);
    private static final Font FONT_BAND_TAG = FontFactory.getFont(FontFactory.HELVETICA, 9, COR_BAND_TAG);
    private static final Font FONT_BAND_META = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, COR_BAND_TAG);
    private static final Font FONT_BAND_META_STRONG = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE);
    private static final Font FONT_KPI_VALUE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 19, COR_BRAND);
    private static final Font FONT_KPI_LABEL = FontFactory.getFont(FontFactory.HELVETICA, 8, COR_TEXTO_SEC);

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
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

        try (Document document = new Document(PageSize.A4.rotate(), 36, 36, 28, 54)) {
            abrirDocumento(document, out, loc);
            secaoCabecalho(document, messages.resolve("report.loans.title", loc), inicio, fim, loc);

            ZoneOffset offset = OffsetDateTime.now().getOffset();
            OffsetDateTime inicioDT = (inicio != null) ? inicio.atStartOfDay().atOffset(offset) : null;
            OffsetDateTime fimDT = (fim != null) ? fim.atTime(LocalTime.MAX).atOffset(offset) : null;

            List<Loan> emprestimos = loanRepository.findForReport(
                    inicioDT, fimDT, status, prepararFiltroLike(matriculaAluno),
                    idCurso, prepararFiltroLike(isbnOuTombo), idModulo);

            long ativos = emprestimos.stream().filter(e -> e.getStatus() == LoanStatus.ACTIVE).count();
            long atrasados = emprestimos.stream().filter(e -> e.getStatus() == LoanStatus.OVERDUE).count();
            long concluidos = emprestimos.stream().filter(e -> e.getStatus() == LoanStatus.COMPLETED).count();
            adicionarKpis(document,
                    new Kpi(messages.resolve("report.loans.kpi.total", loc), String.valueOf(emprestimos.size())),
                    new Kpi(messages.resolve("report.loans.kpi.active", loc), String.valueOf(ativos)),
                    new Kpi(messages.resolve("report.loans.kpi.overdue", loc), String.valueOf(atrasados)),
                    new Kpi(messages.resolve("report.loans.kpi.completed", loc), String.valueOf(concluidos)));

            PdfPTable table = novaTabela(new float[] { 1.2f, 3.5f, 3f, 2.5f, 4f, 2.5f, 2f });
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

            int row = 0;
            for (Loan e : emprestimos) {
                boolean zebra = (row++ % 2) == 1;
                try {
                    table.addCell(celula(e.getId().toString(), zebra));
                    table.addCell(celula(Optional.ofNullable(e.getStudent()).map(Student::getFullName)
                            .orElse(unknownStudent), zebra));
                    table.addCell(celula(Optional.ofNullable(e.getStudent()).map(Student::getCourse)
                            .map(Course::getName).orElse(naFallback), zebra));
                    table.addCell(celula(Optional.ofNullable(e.getStudent()).map(Student::getAcademicModule)
                            .map(AcademicModule::getName).orElse(dashFallback), zebra));
                    String livroTombo = Optional.ofNullable(e.getBookCopy())
                            .map(ex -> Optional.ofNullable(ex.getBook()).map(Book::getTitle).orElse(bookNA)
                                    + " (" + ex.getCopyCode() + ")")
                            .orElse(copyNA);
                    table.addCell(celula(livroTombo, zebra));
                    table.addCell(celula(formatarData(e.getBorrowedAt(), loc), zebra));
                    if (e.getStatus() != null) {
                        table.addCell(loanStatusBadge(e.getStatus(), loc));
                    } else {
                        table.addCell(celula(dashFallback, zebra));
                    }
                } catch (Exception ex) {
                    log.error("Erro ao processar linha do empréstimo ID {}: {}", e.getId(), ex.getMessage());
                    for (int i = 0; i < 7; i++)
                        table.addCell(celula(i == 0 ? errorLabel : dashFallback, zebra));
                }
            }

            document.add(table);
            adicionarRodapeTotal(document, messages.resolve("report.loans.footer.total", loc, emprestimos.size()));
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

        try (Document document = new Document(PageSize.A4.rotate(), 36, 36, 28, 54)) {
            abrirDocumento(document, out, loc);
            secaoCabecalho(document, messages.resolve("report.students.title", loc), dataInicio, dataFim, loc);

            ZoneOffset offset = OffsetDateTime.now().getOffset();
            OffsetDateTime inicio = (dataInicio != null) ? dataInicio.atStartOfDay().atOffset(offset) : null;
            OffsetDateTime fim = (dataFim != null) ? dataFim.atTime(23, 59, 59).atOffset(offset) : null;

            List<Student> alunos = alunoRepository.findForReport(idModulo, idCurso, idTurno, penalidade, inicio, fim);

            long comPenalidade = alunos.stream().filter(a -> a.getPenaltyCode() != null).count();
            adicionarKpis(document,
                    new Kpi(messages.resolve("report.students.kpi.total", loc), String.valueOf(alunos.size())),
                    new Kpi(messages.resolve("report.students.kpi.with-penalty", loc), String.valueOf(comPenalidade)),
                    new Kpi(messages.resolve("report.students.kpi.without-penalty", loc),
                            String.valueOf(alunos.size() - comPenalidade)));

            PdfPTable table = novaTabela(new float[] { 2f, 5f, 3f, 2f, 2.4f, 2f });
            adicionarCelulaHeader(table, messages.resolve("report.students.col.registration", loc));
            adicionarCelulaHeader(table, messages.resolve("report.students.col.name", loc));
            adicionarCelulaHeader(table, messages.resolve("report.students.col.course", loc));
            adicionarCelulaHeader(table, messages.resolve("report.students.col.module", loc));
            adicionarCelulaHeader(table, messages.resolve("report.students.col.penalty", loc));
            adicionarCelulaHeader(table, messages.resolve("report.students.col.loan-count", loc));

            int row = 0;
            for (Student a : alunos) {
                boolean zebra = (row++ % 2) == 1;
                long totalLoans = loanRepository.countByStudent_RegistrationNumberAndStatus(
                        a.getRegistrationNumber(), LoanStatus.ACTIVE)
                        + loanRepository.countByStudent_RegistrationNumberAndStatus(
                                a.getRegistrationNumber(), LoanStatus.COMPLETED)
                        + loanRepository.countByStudent_RegistrationNumberAndStatus(
                                a.getRegistrationNumber(), LoanStatus.OVERDUE);

                table.addCell(celula(a.getRegistrationNumber(), zebra));
                table.addCell(celula(a.getFullName(), zebra));
                table.addCell(celula(Optional.ofNullable(a.getCourse()).map(Course::getName).orElse(naFallback), zebra));
                table.addCell(celula(Optional.ofNullable(a.getAcademicModule()).map(AcademicModule::getName)
                        .orElse(dashFallback), zebra));
                if (a.getPenaltyCode() != null) {
                    table.addCell(penaltyBadge(a.getPenaltyCode(), loc));
                } else {
                    table.addCell(celula(dashFallback, zebra));
                }
                table.addCell(celula(String.valueOf(totalLoans), zebra));
            }

            document.add(table);
            adicionarRodapeTotal(document, messages.resolve("report.students.footer.total", loc, alunos.size()));
        } catch (Exception ex) {
            log.error("Erro ao gerar relatório de alunos filtrados", ex);
            throw new IOException(messages.resolve("report.common.error.generate", loc), ex);
        }
    }

    // ================= RELATÓRIOS DE CURSOS =================

    public void gerarRelatorioCursosGeral(OutputStream out, Locale locale) throws IOException {
        Locale loc = effective(locale);

        try (Document document = new Document(PageSize.A4.rotate(), 36, 36, 28, 54)) {
            abrirDocumento(document, out, loc);
            secaoCabecalho(document, messages.resolve("report.courses.title", loc), null, null, loc);

            List<CourseStatisticsResponse> estatisticas = courseRepository.findStatistics();

            long totalAlunos = estatisticas.stream().mapToLong(CourseStatisticsResponse::getStudentCount).sum();
            long totalEmprestimos = estatisticas.stream().mapToLong(CourseStatisticsResponse::getTotalLoans).sum();
            adicionarKpis(document,
                    new Kpi(messages.resolve("report.courses.kpi.courses", loc), String.valueOf(estatisticas.size())),
                    new Kpi(messages.resolve("report.courses.kpi.students", loc), String.valueOf(totalAlunos)),
                    new Kpi(messages.resolve("report.courses.kpi.loans", loc), String.valueOf(totalEmprestimos)));

            PdfPTable table = novaTabela(new float[] { 3f, 2f, 2f, 2f });
            adicionarCelulaHeader(table, messages.resolve("report.courses.col.course", loc));
            adicionarCelulaHeader(table, messages.resolve("report.courses.col.student-count", loc));
            adicionarCelulaHeader(table, messages.resolve("report.courses.col.loan-count", loc));
            adicionarCelulaHeader(table, messages.resolve("report.courses.col.avg-loans", loc));

            int row = 0;
            for (CourseStatisticsResponse dto : estatisticas) {
                boolean zebra = (row++ % 2) == 1;
                table.addCell(celula(dto.getCourseName(), zebra));
                table.addCell(celula(String.valueOf(dto.getStudentCount()), zebra));
                table.addCell(celula(String.valueOf(dto.getTotalLoans()), zebra));
                table.addCell(celula(String.format(loc, "%.2f", dto.getAvgLoansPerStudent()), zebra));
            }

            document.add(table);
            adicionarRodapeTotal(document, messages.resolve("report.courses.footer.total", loc, estatisticas.size()));
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

        try (Document document = new Document(PageSize.A4.rotate(), 36, 36, 28, 54)) {
            abrirDocumento(document, out, loc);
            secaoCabecalho(document, messages.resolve("report.books.title", loc), dataInicio, dataFim, loc);

            ZoneOffset offset = OffsetDateTime.now().getOffset();
            OffsetDateTime inicio = (dataInicio != null) ? dataInicio.atStartOfDay().atOffset(offset) : null;
            OffsetDateTime fim = (dataFim != null) ? dataFim.atTime(23, 59, 59).atOffset(offset) : null;

            List<Book> livros = livroRepository.findForReport(
                    prepararFiltroLike(genero), prepararFiltroLike(autor), prepararFiltroLike(editora),
                    cdd, classificacaoEtaria, tipoCapa, inicio, fim);

            // Count copies once per book (reused for the KPI sum and the table column).
            Map<UUID, Long> copias = new LinkedHashMap<>();
            long totalExemplares = 0;
            for (Book l : livros) {
                long c = exemplarRepository.countByBook_Id(l.getId());
                copias.put(l.getId(), c);
                totalExemplares += c;
            }
            adicionarKpis(document,
                    new Kpi(messages.resolve("report.books.kpi.titles", loc), String.valueOf(livros.size())),
                    new Kpi(messages.resolve("report.books.kpi.copies", loc), String.valueOf(totalExemplares)));

            PdfPTable table = novaTabela(new float[] { 1.2f, 4f, 3f, 3f, 2f, 2f });
            adicionarCelulaHeader(table, messages.resolve("report.books.col.id", loc));
            adicionarCelulaHeader(table, messages.resolve("report.books.col.title", loc));
            adicionarCelulaHeader(table, messages.resolve("report.books.col.author", loc));
            adicionarCelulaHeader(table, messages.resolve("report.books.col.genres", loc));
            adicionarCelulaHeader(table, messages.resolve("report.books.col.dewey", loc));
            adicionarCelulaHeader(table, messages.resolve("report.books.col.copy-count", loc));

            int row = 0;
            for (Book l : livros) {
                boolean zebra = (row++ % 2) == 1;
                table.addCell(celula(l.getId().toString(), zebra));
                table.addCell(celula(l.getTitle(), zebra));
                table.addCell(celula(l.getAuthor(), zebra));
                String generos = l.getGenres().stream().map(Genre::getName).collect(Collectors.joining(", "));
                table.addCell(celula(generos.isEmpty() ? dashFallback : generos, zebra));
                table.addCell(celula(Optional.ofNullable(l.getDeweyClassification()).map(DeweyClassification::getCode)
                        .orElse(dashFallback), zebra));
                table.addCell(celula(String.valueOf(copias.getOrDefault(l.getId(), 0L)), zebra));
            }

            document.add(table);
            adicionarRodapeTotal(document, messages.resolve("report.books.footer.total", loc, livros.size()));
        } catch (Exception ex) {
            log.error("Erro ao gerar relatório de livros filtrados", ex);
            throw new IOException(messages.resolve("report.common.error.generate", loc), ex);
        }
    }

    public void gerarRelatorioEstatisticasLivros(OutputStream out, Locale locale) throws IOException {
        Locale loc = effective(locale);

        try (Document document = new Document(PageSize.A4, 36, 36, 28, 54)) {
            abrirDocumento(document, out, loc);
            secaoCabecalho(document, messages.resolve("report.books-statistics.title", loc), null, null, loc);

            long totalTitulos = livroRepository.count();
            List<Map<String, Object>> porAutor = livroRepository.countByAutor();
            List<Map<String, Object>> porGenero = livroRepository.countByGenero();

            adicionarKpis(document,
                    new Kpi(messages.resolve("report.books-statistics.kpi.titles", loc), String.valueOf(totalTitulos)),
                    new Kpi(messages.resolve("report.books-statistics.kpi.authors", loc), String.valueOf(porAutor.size())),
                    new Kpi(messages.resolve("report.books-statistics.kpi.genres", loc), String.valueOf(porGenero.size())));

            document.add(secao(messages.resolve("report.books-statistics.section.top-authors", loc)));
            PdfPTable tAutor = novaTabela(new float[] { 5f, 2f });
            adicionarCelulaHeader(tAutor, messages.resolve("report.books-statistics.col.author", loc));
            adicionarCelulaHeader(tAutor, messages.resolve("report.books-statistics.col.title-count", loc));
            int[] rowA = { 0 };
            porAutor.stream().limit(10).forEach(e -> {
                boolean zebra = (rowA[0]++ % 2) == 1;
                tAutor.addCell(celula(String.valueOf(e.get("autor")), zebra));
                tAutor.addCell(celula(String.valueOf(e.get("total")), zebra));
            });
            document.add(tAutor);
            document.add(Chunk.NEWLINE);

            document.add(secao(messages.resolve("report.books-statistics.section.titles-by-genre", loc)));
            PdfPTable tGenero = novaTabela(new float[] { 5f, 2f });
            adicionarCelulaHeader(tGenero, messages.resolve("report.books-statistics.col.genre", loc));
            adicionarCelulaHeader(tGenero, messages.resolve("report.books-statistics.col.title-count", loc));
            int[] rowG = { 0 };
            porGenero.forEach(e -> {
                boolean zebra = (rowG[0]++ % 2) == 1;
                tGenero.addCell(celula(String.valueOf(e.get("genero")), zebra));
                tGenero.addCell(celula(String.valueOf(e.get("total")), zebra));
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

        try (Document document = new Document(PageSize.A4.rotate(), 36, 36, 28, 54)) {
            abrirDocumento(document, out, loc);
            secaoCabecalho(document, messages.resolve("report.copies.title", loc), dataInicio, dataFim, loc);

            ZoneOffset offset = OffsetDateTime.now().getOffset();
            OffsetDateTime inicio = (dataInicio != null) ? dataInicio.atStartOfDay().atOffset(offset) : null;
            OffsetDateTime fim = (dataFim != null) ? dataFim.atTime(23, 59, 59).atOffset(offset) : null;

            List<BookCopy> exemplares = exemplarRepository.findForReport(
                    status, prepararFiltroLike(isbnOuTombo), inicio, fim);

            long disponiveis = exemplares.stream().filter(e -> e.getStatus() == BookCopyStatus.AVAILABLE).count();
            long emprestados = exemplares.stream().filter(e -> e.getStatus() == BookCopyStatus.BORROWED).count();
            adicionarKpis(document,
                    new Kpi(messages.resolve("report.copies.kpi.total", loc), String.valueOf(exemplares.size())),
                    new Kpi(messages.resolve("report.copies.kpi.available", loc), String.valueOf(disponiveis)),
                    new Kpi(messages.resolve("report.copies.kpi.borrowed", loc), String.valueOf(emprestados)));

            PdfPTable table = novaTabela(new float[] { 2f, 4f, 2.5f, 3f, 3f });
            adicionarCelulaHeader(table, messages.resolve("report.copies.col.copy-code", loc));
            adicionarCelulaHeader(table, messages.resolve("report.copies.col.book-title", loc));
            adicionarCelulaHeader(table, messages.resolve("report.copies.col.status", loc));
            adicionarCelulaHeader(table, messages.resolve("report.copies.col.shelf-location", loc));
            adicionarCelulaHeader(table, messages.resolve("report.copies.col.isbn", loc));

            int row = 0;
            for (BookCopy ex : exemplares) {
                boolean zebra = (row++ % 2) == 1;
                table.addCell(celula(ex.getCopyCode(), zebra));
                table.addCell(celula(Optional.ofNullable(ex.getBook()).map(Book::getTitle).orElse(naFallback), zebra));
                if (ex.getStatus() != null) {
                    table.addCell(copyStatusBadge(ex.getStatus(), loc));
                } else {
                    table.addCell(celula(dashFallback, zebra));
                }
                table.addCell(celula(Optional.ofNullable(ex.getShelfLocation()).orElse(dashFallback), zebra));
                table.addCell(celula(Optional.ofNullable(ex.getBook()).map(Book::getIsbn).orElse(dashFallback), zebra));
            }

            document.add(table);
            adicionarRodapeTotal(document, messages.resolve("report.copies.footer.total", loc, exemplares.size()));
        } catch (Exception ex) {
            log.error("Erro ao gerar relatório de exemplares filtrados", ex);
            throw new IOException(messages.resolve("report.common.error.generate", loc), ex);
        }
    }

    // ================= MÉTODOS AUXILIARES (HELPERS) =================

    private record Kpi(String label, String value) {
    }

    /** Opens the PDF with a branded per-page footer. Must precede any content. */
    private void abrirDocumento(Document document, OutputStream out, Locale locale) throws DocumentException {
        PdfWriter writer = PdfWriter.getInstance(document, out);
        String rodapeEsquerda = messages.resolve("report.common.brand", locale) + "  ·  "
                + messages.resolve("report.common.generated-at", locale, agora(locale));
        String confidential = messages.resolve("report.common.confidential", locale);
        String prefixoPagina = messages.resolve("report.common.page", locale, "").trim();
        writer.setPageEvent(new ReportFooterEvent(rodapeEsquerda, confidential, prefixoPagina));
        document.open();
    }

    /** Brand header band (logo + wordmark + tagline · timestamp + confidential), title and period. */
    private void secaoCabecalho(Document document, String titulo, LocalDate inicio, LocalDate fim, Locale locale)
            throws DocumentException {
        PdfPTable band = new PdfPTable(2);
        band.setWidthPercentage(100);
        band.setWidths(new float[] { 7f, 3f });

        PdfPCell left = new PdfPCell();
        left.setBackgroundColor(COR_BRAND);
        left.setBorder(Rectangle.NO_BORDER);
        left.setPaddingTop(14f);
        left.setPaddingBottom(14f);
        left.setPaddingLeft(20f);
        Paragraph brandLine = new Paragraph();
        brandLine.setLeading(20f);
        Image logo = carregarImagem("/report/report-logo-band.png");
        if (logo != null) {
            logo.scaleToFit(24, 24);
            brandLine.add(new Chunk(logo, 0, -6));
            brandLine.add(new Chunk("   "));
        }
        brandLine.add(new Chunk(messages.resolve("report.common.brand", locale), FONT_BAND_BRAND));
        left.addElement(brandLine);
        left.addElement(new Paragraph(messages.resolve("report.common.brand.tagline", locale), FONT_BAND_TAG));
        band.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBackgroundColor(COR_BRAND);
        right.setBorder(Rectangle.NO_BORDER);
        right.setPaddingTop(16f);
        right.setPaddingBottom(14f);
        right.setPaddingRight(20f);
        Paragraph gen = new Paragraph(
                messages.resolve("report.common.generated-at", locale, agora(locale)), FONT_BAND_META);
        gen.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(gen);
        Paragraph conf = new Paragraph(messages.resolve("report.common.confidential", locale), FONT_BAND_META_STRONG);
        conf.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(conf);
        band.addCell(right);

        document.add(band);

        Paragraph pTitulo = new Paragraph(titulo, FONT_TITULO);
        pTitulo.setSpacingBefore(18);
        pTitulo.setSpacingAfter(2);
        document.add(pTitulo);

        String periodoStr = (inicio != null && fim != null)
                ? messages.resolve("report.period.range", locale,
                        inicio.format(dateFmt(locale)), fim.format(dateFmt(locale)))
                : messages.resolve("report.period.all", locale);
        Paragraph pPeriodo = new Paragraph(periodoStr, FONT_META);
        pPeriodo.setSpacingAfter(16);
        document.add(pPeriodo);
    }

    /** Renders a row of KPI summary tiles (big value + caption). */
    private void adicionarKpis(Document document, Kpi... kpis) throws DocumentException {
        if (kpis.length == 0) {
            return;
        }
        PdfPTable t = new PdfPTable(kpis.length);
        t.setWidthPercentage(100);
        t.setSpacingAfter(18);
        for (Kpi k : kpis) {
            PdfPCell c = new PdfPCell();
            c.setBackgroundColor(COR_KPI_BG);
            c.setBorderColor(Color.WHITE);
            c.setBorderWidth(4f);
            c.setPadding(13f);
            Paragraph val = new Paragraph(k.value(), FONT_KPI_VALUE);
            Paragraph lab = new Paragraph(k.label().toUpperCase(Locale.ROOT), FONT_KPI_LABEL);
            lab.setSpacingBefore(2f);
            c.addElement(val);
            c.addElement(lab);
            t.addCell(c);
        }
        document.add(t);
    }

    private Paragraph secao(String texto) {
        Paragraph p = new Paragraph(texto, FONT_SECAO);
        p.setSpacingBefore(6);
        p.setSpacingAfter(8);
        return p;
    }

    private Image carregarImagem(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            return (is == null) ? null : Image.getInstance(is.readAllBytes());
        } catch (Exception e) {
            log.warn("Imagem do relatório indisponível ({}): {}", path, e.getMessage());
            return null;
        }
    }

    /** Renders the footer (brand · timestamp left, confidential · page right) on every page. */
    private static class ReportFooterEvent extends PdfPageEventHelper {
        private final String textoEsquerda;
        private final String confidential;
        private final String prefixoPagina;

        ReportFooterEvent(String textoEsquerda, String confidential, String prefixoPagina) {
            this.textoEsquerda = textoEsquerda;
            this.confidential = confidential;
            this.prefixoPagina = prefixoPagina;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float y = document.bottom() - 16;
            cb.setColorStroke(COR_RODAPE_LINHA);
            cb.setLineWidth(0.5f);
            cb.moveTo(document.left(), y + 9);
            cb.lineTo(document.right(), y + 9);
            cb.stroke();
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(textoEsquerda, FONT_RODAPE), document.left(), y, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase(confidential + "  ·  " + prefixoPagina + " " + writer.getPageNumber(), FONT_RODAPE),
                    document.right(), y, 0);
        }
    }

    private void adicionarRodapeTotal(Document document, String texto) throws DocumentException {
        Paragraph pTotal = new Paragraph(texto, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COR_TEXTO));
        pTotal.setAlignment(Element.ALIGN_RIGHT);
        pTotal.setSpacingBefore(14);
        document.add(pTotal);
    }

    private PdfPTable novaTabela(float[] widths) throws DocumentException {
        PdfPTable table = new PdfPTable(widths.length);
        table.setWidthPercentage(100);
        table.setWidths(widths);
        table.setHeaderRows(1);
        return table;
    }

    private void adicionarCelulaHeader(PdfPTable table, String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FONT_CABECALHO_TABELA));
        cell.setBackgroundColor(COR_BRAND);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        cell.setBorderColor(COR_BRAND);
        table.addCell(cell);
    }

    private PdfPCell celula(String texto, boolean zebra) {
        PdfPCell cell = new PdfPCell(new Phrase(texto != null ? texto : "", FONT_CORPO_TABELA));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6.5f);
        cell.setBorderColor(COR_CELULA_BORDA);
        cell.setBackgroundColor(zebra ? COR_ZEBRA : Color.WHITE);
        return cell;
    }

    private PdfPCell badge(String label, Color fg, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, fg)));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        cell.setBorderColor(Color.WHITE);
        cell.setBorderWidth(2f);
        return cell;
    }

    private PdfPCell loanStatusBadge(LoanStatus status, Locale loc) {
        String label = messages.resolve("enum.loan-status." + status.name(), loc);
        return switch (status) {
            case ACTIVE -> badge(label, FG_GREEN, BG_GREEN);
            case OVERDUE -> badge(label, FG_RED, BG_RED);
            case COMPLETED -> badge(label, FG_BLUE, BG_BLUE);
        };
    }

    private PdfPCell copyStatusBadge(BookCopyStatus status, Locale loc) {
        String label = messages.resolve("enum.book-copy-status." + status.name(), loc);
        return switch (status) {
            case AVAILABLE -> badge(label, FG_GREEN, BG_GREEN);
            case BORROWED -> badge(label, FG_AMBER, BG_AMBER);
            case MAINTENANCE -> badge(label, FG_AMBER, BG_AMBER);
            case UNAVAILABLE -> badge(label, FG_GRAY, BG_GRAY);
        };
    }

    private PdfPCell penaltyBadge(PenaltyCode penalty, Locale loc) {
        String label = messages.resolve("enum.penalty-code." + penalty.name(), loc);
        Color fg = (penalty == PenaltyCode.RECORD || penalty == PenaltyCode.WARNING) ? FG_AMBER : FG_RED;
        Color bg = (penalty == PenaltyCode.RECORD || penalty == PenaltyCode.WARNING) ? BG_AMBER : BG_RED;
        return badge(label, fg, bg);
    }

    private String agora(Locale locale) {
        return OffsetDateTime.now().format(dateFmt(locale)) + " " + OffsetDateTime.now().format(TIME_FORMATTER);
    }

    private String formatarData(OffsetDateTime data, Locale locale) {
        return (data != null) ? data.format(dateFmt(locale)) : messages.resolve("report.common.fallback.na", locale);
    }

    private static DateTimeFormatter dateFmt(Locale locale) {
        boolean pt = locale != null && "pt".equalsIgnoreCase(locale.getLanguage());
        return DateTimeFormatter.ofPattern(pt ? "dd/MM/yyyy" : "MM/dd/yyyy");
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
