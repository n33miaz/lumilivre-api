package br.com.lumilivre.api.service;

import br.com.lumilivre.api.model.*;
import br.com.lumilivre.api.repository.AlunoRepository;
import br.com.lumilivre.api.repository.CursoRepository;
import br.com.lumilivre.api.repository.EmprestimoRepository;
import br.com.lumilivre.api.repository.ExemplarRepository;
import br.com.lumilivre.api.repository.LivroRepository;
import br.com.lumilivre.api.dto.curso.CursoEstatisticaResponse;
import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import br.com.lumilivre.api.enums.StatusLivro;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RelatorioService {

    private static final Logger log = LoggerFactory.getLogger(RelatorioService.class);

    private final EmprestimoRepository emprestimoRepository;
    private final AlunoRepository alunoRepository;
    private final LivroRepository livroRepository;
    private final CursoRepository cursoRepository;
    private final ExemplarRepository exemplarRepository;

    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font FONT_CABECALHO_TABELA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
    private static final Font FONT_CORPO_TABELA = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Color COR_CABECALHO_TABELA = new Color(118, 32, 117);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public RelatorioService(EmprestimoRepository emprestimoRepository, AlunoRepository alunoRepository,
            LivroRepository livroRepository, CursoRepository cursoRepository, ExemplarRepository exemplarRepository) {
        this.emprestimoRepository = emprestimoRepository;
        this.alunoRepository = alunoRepository;
        this.livroRepository = livroRepository;
        this.cursoRepository = cursoRepository;
        this.exemplarRepository = exemplarRepository;
    }

    // ================= RELATÓRIOS DE EMPRÉSTIMOS =================

    public void gerarRelatorioEmprestimosPorFiltros(OutputStream out, LocalDate inicio, LocalDate fim,
            StatusEmprestimo status, String matriculaAluno, Integer idCurso,
            String isbnOuTombo, Integer idModulo) throws IOException {
        try (Document document = new Document(PageSize.A4.rotate())) {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "Relatório de Empréstimos", inicio, fim);

            List<EmprestimoModel> emprestimos = emprestimoRepository.findForReport(
                    inicio, fim, status, matriculaAluno, idCurso, isbnOuTombo, idModulo);

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

            for (EmprestimoModel e : emprestimos) {
                table.addCell(criarCelulaDados(String.valueOf(e.getId())));
                table.addCell(criarCelulaDados(
                        Optional.ofNullable(e.getAluno()).map(AlunoModel::getNomeCompleto).orElse("N/A")));
                table.addCell(criarCelulaDados(Optional.ofNullable(e.getAluno()).map(AlunoModel::getCurso)
                        .map(CursoModel::getNome).orElse("N/A")));
                table.addCell(criarCelulaDados(Optional.ofNullable(e.getAluno()).map(AlunoModel::getModulo)
                        .map(ModuloModel::getNome).orElse("-")));
                String livroTombo = Optional.ofNullable(e.getExemplar())
                        .map(ex -> ex.getLivro().getNome() + " (" + ex.getTombo() + ")").orElse("N/A");
                table.addCell(criarCelulaDados(livroTombo));
                table.addCell(criarCelulaDados(formatarData(e.getDataEmprestimo())));
                table.addCell(
                        criarCelulaDados(Optional.ofNullable(e.getStatusEmprestimo()).map(Enum::name).orElse("-")));
            }

            document.add(table);
            adicionarRodapeRelatorio(document, "Total de empréstimos filtrados: " + emprestimos.size());
        } catch (Exception ex) {
            log.error("Erro ao gerar relatório de empréstimos filtrados", ex);
            throw new IOException("Erro ao gerar relatório de empréstimos filtrados", ex);
        }
    }

    // ================= RELATÓRIOS DE ALUNOS =================

    public void gerarRelatorioAlunosPorFiltros(OutputStream out, Integer idModulo, Integer idCurso,
            Integer idTurno, Penalidade penalidade) throws IOException {
        try (Document document = new Document(PageSize.A4.rotate())) {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "Relatório de Alunos", null, null);

            List<AlunoModel> alunos = alunoRepository.findForReport(idModulo, idCurso, idTurno, penalidade);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 2f, 5f, 3f, 2f, 2f, 2f });

            adicionarCelulaHeader(table, "Matrícula");
            adicionarCelulaHeader(table, "Nome");
            adicionarCelulaHeader(table, "Curso");
            adicionarCelulaHeader(table, "Módulo");
            adicionarCelulaHeader(table, "Penalidade");
            adicionarCelulaHeader(table, "Qtd. Empréstimos");

            for (AlunoModel a : alunos) {
                table.addCell(criarCelulaDados(a.getMatricula()));
                table.addCell(criarCelulaDados(a.getNomeCompleto()));
                table.addCell(
                        criarCelulaDados(Optional.ofNullable(a.getCurso()).map(CursoModel::getNome).orElse("N/A")));
                table.addCell(
                        criarCelulaDados(Optional.ofNullable(a.getModulo()).map(ModuloModel::getNome).orElse("-")));
                table.addCell(criarCelulaDados(Optional.ofNullable(a.getPenalidade()).map(Enum::name).orElse("-")));
                table.addCell(criarCelulaDados(String.valueOf(a.getEmprestimosCount())));
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

            List<CursoEstatisticaResponse> estatisticas = cursoRepository.findEstatisticasCursos();

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
            String cdd, String classificacaoEtaria, String tipoCapa) throws IOException {
        try (Document document = new Document(PageSize.A4.rotate())) {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "Relatório de Livros", null, null);

            List<LivroModel> livros = livroRepository.findForReport(
                    genero, autor, cdd, classificacaoEtaria, tipoCapa);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 1.2f, 4f, 3f, 3f, 2f, 2f });

            adicionarCelulaHeader(table, "ID");
            adicionarCelulaHeader(table, "Título");
            adicionarCelulaHeader(table, "Autor");
            adicionarCelulaHeader(table, "Gêneros");
            adicionarCelulaHeader(table, "CDD");
            adicionarCelulaHeader(table, "Qtd. Exemplares");

            for (LivroModel l : livros) {
                table.addCell(criarCelulaDados(String.valueOf(l.getId())));
                table.addCell(criarCelulaDados(l.getNome()));
                table.addCell(criarCelulaDados(l.getAutor()));
                String generos = l.getGeneros().stream().map(GeneroModel::getNome).collect(Collectors.joining(", "));
                table.addCell(criarCelulaDados(generos.isEmpty() ? "-" : generos));
                table.addCell(criarCelulaDados(Optional.ofNullable(l.getCdd()).map(CddModel::getCodigo).orElse("-")));
                table.addCell(criarCelulaDados(String.valueOf(l.getQuantidade())));
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

            // Tabela de resumo
            PdfPTable tableResumo = new PdfPTable(2);
            tableResumo.setWidthPercentage(50);
            adicionarCelulaHeader(tableResumo, "Métrica");
            adicionarCelulaHeader(tableResumo, "Valor");
            tableResumo.addCell(criarCelulaDados("Total de Títulos Distintos"));
            tableResumo.addCell(criarCelulaDados(String.valueOf(totalTitulos)));
            document.add(tableResumo);
            document.add(Chunk.NEWLINE);

            // Tabela por autor (Top 10)
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

            // Tabela por gênero
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

    public void gerarRelatorioExemplaresFiltrados(OutputStream out, StatusLivro status, String isbnOuTombo)
            throws IOException {
        try (Document document = new Document(PageSize.A4.rotate())) {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "Relatório de Exemplares", null, null);

            List<ExemplarModel> exemplares = exemplarRepository.findForReport(status, isbnOuTombo);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 2f, 4f, 2.5f, 3f, 3f });

            adicionarCelulaHeader(table, "Tombo");
            adicionarCelulaHeader(table, "Título do Livro");
            adicionarCelulaHeader(table, "Status");
            adicionarCelulaHeader(table, "Localização Física");
            adicionarCelulaHeader(table, "ISBN");

            for (ExemplarModel ex : exemplares) {
                table.addCell(criarCelulaDados(ex.getTombo()));
                table.addCell(
                        criarCelulaDados(Optional.ofNullable(ex.getLivro()).map(LivroModel::getNome).orElse("N/A")));
                table.addCell(criarCelulaDados(Optional.ofNullable(ex.getStatus_livro()).map(Enum::name).orElse("-")));
                table.addCell(criarCelulaDados(Optional.ofNullable(ex.getLocalizacao_fisica()).orElse("-")));
                table.addCell(
                        criarCelulaDados(Optional.ofNullable(ex.getLivro()).map(LivroModel::getIsbn).orElse("-")));
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
        PdfPCell cell = new PdfPCell(new Phrase(texto, FONT_CORPO_TABELA));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setBorderColor(Color.LIGHT_GRAY);
        return cell;
    }

    private String formatarData(LocalDateTime data) {
        return (data != null) ? data.format(DATE_FORMATTER) : "N/A";
    }
}