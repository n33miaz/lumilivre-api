package br.com.lumilivre.api.service;

import br.com.lumilivre.api.model.*;
import br.com.lumilivre.api.repository.StudentRepository;
import br.com.lumilivre.api.repository.CourseRepository;
import br.com.lumilivre.api.repository.LoanRepository;
import br.com.lumilivre.api.repository.BookCopyRepository;
import br.com.lumilivre.api.repository.BookRepository;
import br.com.lumilivre.api.dto.v1.curso.CursoEstatisticaResponse;
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

    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font FONT_CABECALHO_TABELA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
    private static final Font FONT_CORPO_TABELA = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Color COR_CABECALHO_TABELA = new Color(118, 32, 117);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ReportService(LoanRepository loanRepository, StudentRepository alunoRepository,
            BookRepository livroRepository, CourseRepository courseRepository, BookCopyRepository exemplarRepository) {
        this.loanRepository = loanRepository;
        this.alunoRepository = alunoRepository;
        this.livroRepository = livroRepository;
        this.courseRepository = courseRepository;
        this.exemplarRepository = exemplarRepository;
    }

    // ================= RELATÓRIOS DE EMPRÉSTIMOS =================

    public void gerarRelatorioEmprestimosPorFiltros(OutputStream out, LocalDate inicio, LocalDate fim,
            LoanStatus status, String matriculaAluno, Integer idCurso,
            String isbnOuTombo, Integer idModulo) throws IOException {
        try (Document document = new Document(PageSize.A4.rotate())) {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "Relatório de Empréstimos", inicio, fim);

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

            adicionarCelulaHeader(table, "ID");
            adicionarCelulaHeader(table, "Aluno");
            adicionarCelulaHeader(table, "Curso");
            adicionarCelulaHeader(table, "Módulo");
            adicionarCelulaHeader(table, "Livro / Tombo");
            adicionarCelulaHeader(table, "Data Empréstimo");
            adicionarCelulaHeader(table, "Status");

            for (Loan e : emprestimos) {
                try {
                    table.addCell(criarCelulaDados(e.getId().toString()));

                    String nomeAluno = Optional.ofNullable(e.getStudent())
                            .map(Student::getFullName)
                            .orElse("Aluno Desconhecido");
                    table.addCell(criarCelulaDados(nomeAluno));

                    String nomeCurso = Optional.ofNullable(e.getStudent())
                            .map(Student::getCourse)
                            .map(Course::getName)
                            .orElse("N/A");
                    table.addCell(criarCelulaDados(nomeCurso));

                    String nomeModulo = Optional.ofNullable(e.getStudent())
                            .map(Student::getAcademicModule)
                            .map(AcademicModule::getName)
                            .orElse("-");
                    table.addCell(criarCelulaDados(nomeModulo));

                    String livroTombo = Optional.ofNullable(e.getBookCopy())
                            .map(ex -> {
                                String nomeLivro = Optional.ofNullable(ex.getBook())
                                        .map(Book::getTitle)
                                        .orElse("Livro N/A");
                                return nomeLivro + " (" + ex.getCopyCode() + ")";
                            })
                            .orElse("Exemplar N/A");
                    table.addCell(criarCelulaDados(livroTombo));

                    table.addCell(criarCelulaDados(formatarData(e.getBorrowedAt())));

                    String statusStr = Optional.ofNullable(e.getStatus())
                            .map(Enum::name)
                            .orElse("-");
                    table.addCell(criarCelulaDados(statusStr));

                } catch (Exception ex) {
                    log.error("Erro ao processar linha do empréstimo ID {}: {}", e.getId(), ex.getMessage());
                    for (int i = 0; i < 7; i++) table.addCell(criarCelulaDados(i == 0 ? "ERRO" : "-"));
                }
            }

            document.add(table);
            adicionarRodapeRelatorio(document, "Total de empréstimos filtrados: " + emprestimos.size());
        } catch (Exception ex) {
            log.error("Erro fatal ao gerar relatório de empréstimos", ex);
            throw new IOException("Erro ao gerar PDF", ex);
        }
    }

    // ================= RELATÓRIOS DE ALUNOS =================

    public void gerarRelatorioAlunosPorFiltros(OutputStream out, Integer idModulo, Integer idCurso,
            Integer idTurno, PenaltyCode penalidade, LocalDate dataInicio, LocalDate dataFim) throws IOException {
        try (Document document = new Document(PageSize.A4.rotate())) {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "Relatório de Alunos", dataInicio, dataFim);

            ZoneOffset offset = OffsetDateTime.now().getOffset();
            OffsetDateTime inicio = (dataInicio != null) ? dataInicio.atStartOfDay().atOffset(offset) : null;
            OffsetDateTime fim = (dataFim != null) ? dataFim.atTime(23, 59, 59).atOffset(offset) : null;

            List<Student> alunos = alunoRepository.findForReport(idModulo, idCurso, idTurno, penalidade, inicio, fim);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 2f, 5f, 3f, 2f, 2f, 2f });

            adicionarCelulaHeader(table, "Matrícula");
            adicionarCelulaHeader(table, "Nome");
            adicionarCelulaHeader(table, "Curso");
            adicionarCelulaHeader(table, "Módulo");
            adicionarCelulaHeader(table, "Penalidade");
            adicionarCelulaHeader(table, "Qtd. Empréstimos");

            for (Student a : alunos) {
                long totalLoans = loanRepository.countByStudent_RegistrationNumberAndStatus(a.getRegistrationNumber(), LoanStatus.ACTIVE)
                        + loanRepository.countByStudent_RegistrationNumberAndStatus(a.getRegistrationNumber(), LoanStatus.COMPLETED)
                        + loanRepository.countByStudent_RegistrationNumberAndStatus(a.getRegistrationNumber(), LoanStatus.OVERDUE);

                table.addCell(criarCelulaDados(a.getRegistrationNumber()));
                table.addCell(criarCelulaDados(a.getFullName()));
                table.addCell(criarCelulaDados(Optional.ofNullable(a.getCourse()).map(Course::getName).orElse("N/A")));
                table.addCell(criarCelulaDados(Optional.ofNullable(a.getAcademicModule()).map(AcademicModule::getName).orElse("-")));
                table.addCell(criarCelulaDados(Optional.ofNullable(a.getPenaltyCode()).map(Enum::name).orElse("-")));
                table.addCell(criarCelulaDados(String.valueOf(totalLoans)));
            }

            document.add(table);
            adicionarRodapeRelatorio(document, "Total de alunos: " + alunos.size());
        } catch (Exception ex) {
            log.error("Erro ao gerar relatório de alunos filtrados", ex);
            throw new IOException("Erro ao gerar relatório de alunos filtrados", ex);
        }
    }

    // ================= RELATÓRIOS DE CURSOS =================

    public void gerarRelatorioCursosGeral(OutputStream out) throws IOException {
        try (Document document = new Document(PageSize.A4.rotate())) {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "Relatório Geral de Cursos", null, null);

            List<CursoEstatisticaResponse> estatisticas = courseRepository.findEstatisticasCursos();

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 3f, 2f, 2f, 2f });

            adicionarCelulaHeader(table, "Curso");
            adicionarCelulaHeader(table, "Qtd. Alunos");
            adicionarCelulaHeader(table, "Qtd. Empréstimos");
            adicionarCelulaHeader(table, "Média Empréstimos/Aluno");

            for (CursoEstatisticaResponse dto : estatisticas) {
                table.addCell(criarCelulaDados(dto.getNomeCurso()));
                table.addCell(criarCelulaDados(String.valueOf(dto.getQuantidadeAlunos())));
                table.addCell(criarCelulaDados(String.valueOf(dto.getTotalEmprestimos())));
                table.addCell(criarCelulaDados(String.format("%.2f", dto.getMediaEmprestimosPorAluno())));
            }

            document.add(table);
            adicionarRodapeRelatorio(document, "Total de cursos: " + estatisticas.size());
        } catch (Exception ex) {
            log.error("Erro ao gerar relatório geral de cursos", ex);
            throw new IOException("Erro ao gerar relatório geral de cursos", ex);
        }
    }

    // ================= RELATÓRIOS DE LIVROS E EXEMPLARES =================

    public void gerarRelatorioLivrosFiltrados(OutputStream out, String genero, String autor,
            String editora, String cdd, String classificacaoEtaria, String tipoCapa, LocalDate dataInicio, LocalDate dataFim)
            throws IOException {
        try (Document document = new Document(PageSize.A4.rotate())) {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "Relatório de Livros", dataInicio, dataFim);

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

            adicionarCelulaHeader(table, "ID");
            adicionarCelulaHeader(table, "Título");
            adicionarCelulaHeader(table, "Autor");
            adicionarCelulaHeader(table, "Gêneros");
            adicionarCelulaHeader(table, "CDD");
            adicionarCelulaHeader(table, "Qtd. Exemplares");

            for (Book l : livros) {
                long qtdExemplares = exemplarRepository.countByBook_Id(l.getId());
                table.addCell(criarCelulaDados(l.getId().toString()));
                table.addCell(criarCelulaDados(l.getTitle()));
                table.addCell(criarCelulaDados(l.getAuthor()));
                String generos = l.getGenres().stream().map(Genre::getName).collect(Collectors.joining(", "));
                table.addCell(criarCelulaDados(generos.isEmpty() ? "-" : generos));
                table.addCell(criarCelulaDados(Optional.ofNullable(l.getDeweyClassification()).map(DeweyClassification::getCode).orElse("-")));
                table.addCell(criarCelulaDados(String.valueOf(qtdExemplares)));
            }

            document.add(table);
            adicionarRodapeRelatorio(document, "Total de títulos: " + livros.size());
        } catch (Exception ex) {
            log.error("Erro ao gerar relatório de livros filtrados", ex);
            throw new IOException("Erro ao gerar relatório de livros filtrados", ex);
        }
    }

    public void gerarRelatorioEstatisticasLivros(OutputStream out) throws IOException {
        try (Document document = new Document(PageSize.A4)) {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "Estatísticas de Livros", null, null);

            long totalTitulos = livroRepository.count();
            List<Map<String, Object>> porAutor = livroRepository.countByAutor();
            List<Map<String, Object>> porGenero = livroRepository.countByGenero();

            PdfPTable tableResumo = new PdfPTable(2);
            tableResumo.setWidthPercentage(50);
            adicionarCelulaHeader(tableResumo, "Métrica");
            adicionarCelulaHeader(tableResumo, "Valor");
            tableResumo.addCell(criarCelulaDados("Total de Títulos Distintos"));
            tableResumo.addCell(criarCelulaDados(String.valueOf(totalTitulos)));
            document.add(tableResumo);
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("Top 10 Autores com Mais Títulos",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            PdfPTable tAutor = new PdfPTable(2);
            tAutor.setWidthPercentage(80);
            tAutor.setSpacingBefore(10);
            adicionarCelulaHeader(tAutor, "Autor");
            adicionarCelulaHeader(tAutor, "Quantidade de Títulos");
            porAutor.stream().limit(10).forEach(e -> {
                tAutor.addCell(criarCelulaDados(String.valueOf(e.get("autor"))));
                tAutor.addCell(criarCelulaDados(String.valueOf(e.get("total"))));
            });
            document.add(tAutor);
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("Títulos por Gênero", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            PdfPTable tGenero = new PdfPTable(2);
            tGenero.setWidthPercentage(80);
            tGenero.setSpacingBefore(10);
            adicionarCelulaHeader(tGenero, "Gênero");
            adicionarCelulaHeader(tGenero, "Quantidade de Títulos");
            porGenero.forEach(e -> {
                tGenero.addCell(criarCelulaDados(String.valueOf(e.get("genero"))));
                tGenero.addCell(criarCelulaDados(String.valueOf(e.get("total"))));
            });
            document.add(tGenero);

        } catch (Exception ex) {
            log.error("Erro ao gerar estatísticas de livros", ex);
            throw new IOException("Erro ao gerar estatísticas de livros", ex);
        }
    }

    public void gerarRelatorioExemplaresFiltrados(OutputStream out, BookCopyStatus status, String isbnOuTombo,
            LocalDate dataInicio, LocalDate dataFim)
            throws IOException {
        try (Document document = new Document(PageSize.A4.rotate())) {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "Relatório de Exemplares", dataInicio, dataFim);

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

            adicionarCelulaHeader(table, "Tombo");
            adicionarCelulaHeader(table, "Título do Livro");
            adicionarCelulaHeader(table, "Status");
            adicionarCelulaHeader(table, "Localização Física");
            adicionarCelulaHeader(table, "ISBN");

            for (BookCopy ex : exemplares) {
                table.addCell(criarCelulaDados(ex.getCopyCode()));
                table.addCell(criarCelulaDados(Optional.ofNullable(ex.getBook()).map(Book::getTitle).orElse("N/A")));
                table.addCell(criarCelulaDados(Optional.ofNullable(ex.getStatus()).map(Enum::name).orElse("-")));
                table.addCell(criarCelulaDados(Optional.ofNullable(ex.getShelfLocation()).orElse("-")));
                table.addCell(criarCelulaDados(Optional.ofNullable(ex.getBook()).map(Book::getIsbn).orElse("-")));
            }

            document.add(table);
            adicionarRodapeRelatorio(document, "Total de exemplares encontrados: " + exemplares.size());
        } catch (Exception ex) {
            log.error("Erro ao gerar relatório de exemplares filtrados", ex);
            throw new IOException("Erro ao gerar relatório de exemplares filtrados", ex);
        }
    }

    // ================= MÉTODOS AUXILIARES (HELPERS) =================

    private void adicionarCabecalhoRelatorio(Document document, String titulo, LocalDate inicio, LocalDate fim)
            throws DocumentException {
        Paragraph pTitulo = new Paragraph(titulo, FONT_TITULO);
        pTitulo.setAlignment(Element.ALIGN_CENTER);
        pTitulo.setSpacingAfter(10);
        document.add(pTitulo);

        String periodoStr = (inicio != null && fim != null)
                ? "Período: " + inicio.format(DATE_FORMATTER) + " a " + fim.format(DATE_FORMATTER)
                : "Período: Todos os registros";
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

    private String formatarData(OffsetDateTime data) {
        return (data != null) ? data.format(DATE_FORMATTER) : "N/A";
    }

    private String prepararFiltroLike(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        return "%" + valor.trim() + "%";
    }
}
