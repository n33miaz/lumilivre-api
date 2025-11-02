package br.com.lumilivre.api.service;

import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.model.LivroModel;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.OutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Service
public class RelatorioService {

    private final EmprestimoService emprestimoService;
    private final AlunoService alunoService;
    private final LivroService livroService;

    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font FONT_CABECALHO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
    private static final Color COR_CABECALHO = new Color(118, 32, 117);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public RelatorioService(EmprestimoService emprestimoService, AlunoService alunoService, LivroService livroService) {
        this.emprestimoService = emprestimoService;
        this.alunoService = alunoService;
        this.livroService = livroService;
    }

    // --- MÉTODOS PÚBLICOS ---
    public void gerarRelatorioEmprestimos(OutputStream out) throws IOException {
        List<EmprestimoModel> emprestimos = emprestimoService.buscarTodos();
        String[] headers = { "ID", "Aluno", "Livro", "Data Empréstimo" };

        Function<EmprestimoModel, String[]> rowMapper = e -> new String[] {
                String.valueOf(e.getId()),
                Optional.ofNullable(e.getAluno()).map(AlunoModel::getNomeCompleto).orElse("N/A"),
                Optional.ofNullable(e.getExemplar()).map(ex -> ex.getLivro().getNome()).orElse("N/A"),
                formatarData(e.getDataEmprestimo())
        };

        gerarRelatorioPDF(out, "Relatório Geral de Empréstimos", headers, emprestimos, rowMapper);
    }

    public void gerarRelatorioAlunos(OutputStream out) throws IOException {
        List<AlunoModel> alunos = alunoService.buscarTodos();
        String[] headers = { "Matrícula", "Nome", "Qtd. Empréstimos" };

        Function<AlunoModel, String[]> rowMapper = a -> new String[] {
                a.getMatricula(),
                a.getNomeCompleto(),
                String.valueOf(a.getEmprestimosCount())
        };

        gerarRelatorioPDF(out, "Relatório de Alunos", headers, alunos, rowMapper);
    }

    public void gerarRelatorioLivros(OutputStream out) throws IOException {
        List<LivroModel> livros = livroService.buscarTodos();
        String[] headers = { "ID", "Nome", "Autor", "Qtd. Exemplares" };

        Function<LivroModel, String[]> rowMapper = l -> new String[] {
                String.valueOf(l.getId()),
                l.getNome(),
                l.getAutor(),
                String.valueOf(l.getQuantidade())
        };

        gerarRelatorioPDF(out, "Relatório de Livros", headers, livros, rowMapper);
    }

    public void gerarRelatorioEmprestimosAtivosEAtrasados(OutputStream out) throws IOException {
        List<EmprestimoModel> emprestimos = emprestimoService.buscarAtivosEAtrasados();
        String[] headers = { "ID", "Aluno", "Livro", "Data Devolução", "Status" };

        Function<EmprestimoModel, String[]> rowMapper = e -> new String[] {
                String.valueOf(e.getId()),
                Optional.ofNullable(e.getAluno()).map(AlunoModel::getNomeCompleto).orElse("N/A"),
                Optional.ofNullable(e.getExemplar()).map(ex -> ex.getLivro().getNome()).orElse("N/A"),
                formatarData(e.getDataDevolucao()),
                e.getStatusEmprestimo().toString()
        };

        gerarRelatorioPDF(out, "Relatório de Empréstimos Ativos e Atrasados", headers, emprestimos, rowMapper);
    }

    // --- MÉTODO PRIVADO GENÉRICO ---
    private <T> void gerarRelatorioPDF(OutputStream out, String titulo, String[] headers, List<T> dados,
            Function<T, String[]> rowMapper) throws IOException {
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph pTitulo = new Paragraph(titulo, FONT_TITULO);
            pTitulo.setAlignment(Paragraph.ALIGN_CENTER);
            pTitulo.setSpacingAfter(20);
            document.add(pTitulo);

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            table.setWidths(inferirLarguras(headers.length));

            for (String header : headers) {
                table.addCell(criarCelulaHeader(header));
            }

            if (dados != null) {
                for (T item : dados) {
                    String[] rowData = rowMapper.apply(item);
                    for (String cellData : rowData) {
                        table.addCell(criarCelulaDados(cellData));
                    }
                }
            }

            document.add(table);

        } catch (DocumentException ex) {
            throw new IOException("Erro ao gerar o documento PDF: " + titulo, ex);
        } finally {
            document.close();
        }
    }

    // --- MÉTODOS HELPERS PARA ESTILO E FORMATAÇÃO ---
    private PdfPCell criarCelulaHeader(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, FONT_CABECALHO));
        cell.setBackgroundColor(COR_CABECALHO);
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        cell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
        cell.setPadding(8);
        return cell;
    }

    private PdfPCell criarCelulaDados(String texto) {
        PdfPCell cell = new PdfPCell(new Phrase(texto));
        cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
        cell.setVerticalAlignment(PdfPCell.ALIGN_MIDDLE);
        cell.setPadding(6);
        return cell;
    }

    private String formatarData(LocalDateTime data) {
        if (data == null) {
            return "N/A";
        }
        return data.format(DATE_FORMATTER);
    }

    private float[] inferirLarguras(int numColunas) {
        if (numColunas == 4)
            return new float[] { 1f, 3f, 3f, 2f };
        if (numColunas == 3)
            return new float[] { 2f, 4f, 2f };
        if (numColunas == 5)
            return new float[] { 1f, 3f, 3f, 2f, 2f };
        return new float[numColunas];
    }
}