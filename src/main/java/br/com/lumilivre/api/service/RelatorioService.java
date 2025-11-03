package br.com.lumilivre.api.service;

import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.model.ExemplarModel;
import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.enums.StatusEmprestimo;

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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RelatorioService {

    private static final Logger log = LoggerFactory.getLogger(RelatorioService.class);

    private final EmprestimoService emprestimoService;
    private final AlunoService alunoService;
    private final LivroService livroService;
    private final ExemplarService exemplarService;

    // --- CONSTANTES DE ESTILO ---
    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font FONT_CABECALHO_TABELA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
    private static final Font FONT_CORPO_TABELA = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Color COR_CABECALHO_TABELA = new Color(118, 32, 117); // Cor LumiLivre
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public RelatorioService(EmprestimoService emprestimoService, AlunoService alunoService, LivroService livroService,
            ExemplarService exemplarService) {
        this.emprestimoService = emprestimoService;
        this.alunoService = alunoService;
        this.livroService = livroService;
        this.exemplarService = exemplarService;
    }

    // --- MÉTODOS PÚBLICOS PARA GERAR RELATÓRIOS ---

    /**
     * Gera um relatório de empréstimos com filtros opcionais.
     */
    public void gerarRelatorioEmprestimos(OutputStream out, LocalDate inicio, LocalDate fim, StatusEmprestimo status)
            throws IOException {
        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "📚 Relatório de Emprestimos", inicio, fim);

            // NOTA: filtragem deve ser feita no banco para melhor performance.
            List<EmprestimoModel> emprestimos = emprestimoService.buscarTodos().stream()
                    .filter(e -> (inicio == null || !e.getDataEmprestimo().toLocalDate().isBefore(inicio)))
                    .filter(e -> (fim == null || !e.getDataEmprestimo().toLocalDate().isAfter(fim)))
                    .filter(e -> (status == null || e.getStatusEmprestimo() == status))
                    .collect(Collectors.toList());

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 1.5f, 4f, 4f, 4f, 2.5f, 2.5f });

            adicionarCelulaHeader(table, "ID");
            adicionarCelulaHeader(table, "Aluno");
            adicionarCelulaHeader(table, "Curso");
            adicionarCelulaHeader(table, "Livro");
            adicionarCelulaHeader(table, "Data Empréstimo");
            adicionarCelulaHeader(table, "Status");

            for (EmprestimoModel e : emprestimos) {
                table.addCell(criarCelulaDados(String.valueOf(e.getId())));
                table.addCell(criarCelulaDados(
                        Optional.ofNullable(e.getAluno()).map(AlunoModel::getNomeCompleto).orElse("N/A")));
                table.addCell(criarCelulaDados(
                        Optional.ofNullable(e.getAluno()).map(a -> a.getCurso().getNome()).orElse("N/A")));
                table.addCell(criarCelulaDados(
                        Optional.ofNullable(e.getExemplar()).map(ex -> ex.getLivro().getNome()).orElse("N/A")));
                table.addCell(criarCelulaDados(formatarData(e.getDataEmprestimo())));
                table.addCell(criarCelulaDados(e.getStatusEmprestimo().name()));
            }

            document.add(table);
            adicionarRodapeRelatorio(document, "Total de empréstimos: " + emprestimos.size());

        } catch (Exception ex) {
            log.error("Erro ao gerar relatório de empréstimos", ex);
            throw new IOException("Erro ao gerar relatório de empréstimos", ex);
        } finally {
            document.close();
        }
    }

    /**
     * Gera um relatório completo de todos os alunos.
     */
    public void gerarRelatorioAlunos(OutputStream out) throws IOException {
        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "👩‍🎓 Relatório de Alunos", null, null);

            List<AlunoModel> alunos = alunoService.buscarTodos();

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 2f, 5f, 4f, 3f, 2f });

            adicionarCelulaHeader(table, "Matrícula");
            adicionarCelulaHeader(table, "Nome");
            adicionarCelulaHeader(table, "Curso");
            adicionarCelulaHeader(table, "Penalidade");
            adicionarCelulaHeader(table, "Qtd. Empréstimos");

            for (AlunoModel a : alunos) {
                table.addCell(criarCelulaDados(a.getMatricula()));
                table.addCell(criarCelulaDados(a.getNomeCompleto()));
                table.addCell(criarCelulaDados(Optional.ofNullable(a.getCurso()).map(c -> c.getNome()).orElse("N/A")));
                table.addCell(criarCelulaDados(a.getPenalidade() != null ? a.getPenalidade().toString() : "-"));
                table.addCell(criarCelulaDados(String.valueOf(a.getEmprestimosCount())));
            }

            document.add(table);
            adicionarRodapeRelatorio(document, "Total de alunos: " + alunos.size());

        } catch (Exception ex) {
            log.error("Erro ao gerar relatório de alunos", ex);
            throw new IOException("Erro ao gerar relatório de alunos", ex);
        } finally {
            document.close();
        }
    }

    /**
     * Gera um relatório completo de todos os livros (títulos).
     */
    public void gerarRelatorioLivros(OutputStream out) throws IOException {
        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "📖 Relatório de Livros", null, null);

            List<LivroModel> livros = livroService.buscarTodos();

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 2f, 4f, 3f, 4f, 2f });

            adicionarCelulaHeader(table, "ID");
            adicionarCelulaHeader(table, "Título");
            adicionarCelulaHeader(table, "Autor");
            adicionarCelulaHeader(table, "Gêneros");
            adicionarCelulaHeader(table, "Qtd. Exemplares");

            for (LivroModel l : livros) {
                table.addCell(criarCelulaDados(String.valueOf(l.getId())));
                table.addCell(criarCelulaDados(l.getNome()));
                table.addCell(criarCelulaDados(l.getAutor()));
                String generos = l.getGeneros().isEmpty() ? "-"
                        : l.getGeneros().stream().map(g -> g.getNome()).collect(Collectors.joining(", "));
                table.addCell(criarCelulaDados(generos));
                table.addCell(criarCelulaDados(String.valueOf(l.getQuantidade())));
            }

            document.add(table);
            adicionarRodapeRelatorio(document, "Total de títulos de livros: " + livros.size());

        } catch (Exception ex) {
            log.error("Erro ao gerar relatório de livros", ex);
            throw new IOException("Erro ao gerar relatório de livros", ex);
        } finally {
            document.close();
        }
    }

    /**
     * Gera um relatório completo de todos os exemplares.
     */
    public void gerarRelatorioExemplares(OutputStream out) throws IOException {
        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "🏷️ Relatório de Exemplares", null, null);

            List<ExemplarModel> exemplares = exemplarService.buscarTodos();

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 2f, 5f, 3f, 4f });

            adicionarCelulaHeader(table, "Tombo");
            adicionarCelulaHeader(table, "Livro");
            adicionarCelulaHeader(table, "Status");
            adicionarCelulaHeader(table, "Localização");

            for (ExemplarModel e : exemplares) {
                table.addCell(criarCelulaDados(e.getTombo()));
                table.addCell(
                        criarCelulaDados(Optional.ofNullable(e.getLivro()).map(LivroModel::getNome).orElse("N/A")));
                table.addCell(criarCelulaDados(e.getStatus_livro().toString()));
                table.addCell(criarCelulaDados(e.getLocalizacao_fisica()));
            }

            document.add(table);
            adicionarRodapeRelatorio(document, "Total de exemplares: " + exemplares.size());

        } catch (Exception ex) {
            log.error("Erro ao gerar relatório de exemplares", ex);
            throw new IOException("Erro ao gerar relatório de exemplares", ex);
        } finally {
            document.close();
        }
    }

    // --- MÉTODOS HELPERS PRIVADOS ---

    private void adicionarCabecalhoRelatorio(Document document, String titulo, LocalDate inicio, LocalDate fim)
            throws DocumentException {
        Paragraph pTitulo = new Paragraph(titulo, FONT_TITULO);
        pTitulo.setAlignment(Element.ALIGN_CENTER);
        pTitulo.setSpacingAfter(10);
        document.add(pTitulo);

        String periodoStr = (inicio != null && fim != null)
                ? "Período: " + inicio.format(DATE_FORMATTER) + " a " + fim.format(DATE_FORMATTER)
                : "Período: Completo";
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