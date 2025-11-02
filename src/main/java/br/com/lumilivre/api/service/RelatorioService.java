package br.com.lumilivre.api.service;

import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.model.ExemplarModel;
import br.com.lumilivre.api.enums.StatusEmprestimo;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import org.springframework.stereotype.Service;

<<<<<<< Updated upstream
<<<<<<< Updated upstream
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
=======
>>>>>>> Stashed changes
import java.io.OutputStream;
import java.awt.Color;
import java.io.IOException;
<<<<<<< Updated upstream
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
=======
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
>>>>>>> Stashed changes
=======
import java.io.OutputStream;
import java.awt.Color;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
>>>>>>> Stashed changes

@Service
public class RelatorioService {

    private final EmprestimoService emprestimoService;
    private final AlunoService alunoService;
    private final LivroService livroService;
    private final ExemplarService exemplarService;

<<<<<<< Updated upstream
    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font FONT_CABECALHO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
    private static final Color COR_CABECALHO = new Color(118, 32, 117);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public RelatorioService(EmprestimoService emprestimoService, AlunoService alunoService, LivroService livroService) {
=======
    public RelatorioService(EmprestimoService emprestimoService,
                            AlunoService alunoService,
                            LivroService livroService,
                            ExemplarService exemplarService) {
<<<<<<< Updated upstream
>>>>>>> Stashed changes
=======
>>>>>>> Stashed changes
        this.emprestimoService = emprestimoService;
        this.alunoService = alunoService;
        this.livroService = livroService;
        this.exemplarService = exemplarService;
    }

<<<<<<< Updated upstream
<<<<<<< Updated upstream
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
=======
=======
>>>>>>> Stashed changes
    // 🧾 Cabeçalho padrão de relatórios
    private void addCabecalho(Document document, String titulo, LocalDate inicio, LocalDate fim) throws DocumentException {
        Paragraph header = new Paragraph(titulo, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16));
        header.setAlignment(Element.ALIGN_CENTER);
        document.add(header);

        Paragraph periodo = new Paragraph(
            (inicio != null && fim != null)
                ? "Período: " + inicio + " a " + fim
                : "Período: Completo",
            FontFactory.getFont(FontFactory.HELVETICA, 10)
        );
        periodo.setAlignment(Element.ALIGN_CENTER);
        document.add(periodo);
        document.add(new Paragraph(" "));
    }

    // 📚 Relatório de Empréstimos
    public void gerarRelatorioEmprestimos(OutputStream out, LocalDate inicio, LocalDate fim, StatusEmprestimo status) throws IOException {
        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            addCabecalho(document, "📚 Relatório de Empréstimos", inicio, fim);

            List<EmprestimoModel> emprestimos = emprestimoService.buscarTodos();

            if (inicio != null && fim != null) {
                emprestimos = emprestimos.stream()
                    .filter(e -> !e.getDataEmprestimo().toLocalDate().isBefore(inicio)
                              && !e.getDataEmprestimo().toLocalDate().isAfter(fim))
                    .collect(Collectors.toList());
            }

            if (status != null) {
                emprestimos = emprestimos.stream()
                    .filter(e -> e.getStatusEmprestimo() == status)
                    .collect(Collectors.toList());
            }

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 4, 4, 3, 3, 3});

            String[] headers = {"ID", "Aluno", "Curso", "Livro", "Data Empréstimo", "Status"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                table.addCell(cell);
            }

            for (EmprestimoModel e : emprestimos) {
                table.addCell(String.valueOf(e.getId()));
                table.addCell(e.getAluno().getNomeCompleto());
                table.addCell(e.getAluno().getCurso().getNome());
                table.addCell(e.getExemplar().getLivro_isbn().getNome());
                table.addCell(e.getDataEmprestimo().toLocalDate().toString());
                table.addCell(e.getStatusEmprestimo().toString());
            }

            document.add(table);
            document.add(new Paragraph("\nTotal de empréstimos: " + emprestimos.size()));
        } catch (Exception ex) {
            throw new IOException("Erro ao gerar relatório de empréstimos", ex);
        } finally {
            document.close();
        }
>>>>>>> Stashed changes
    }

    // 👩‍🎓 Relatório de Alunos
    public void gerarRelatorioAlunos(OutputStream out) throws IOException {
<<<<<<< Updated upstream
<<<<<<< Updated upstream
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
=======
>>>>>>> Stashed changes
=======
>>>>>>> Stashed changes
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
<<<<<<< Updated upstream
<<<<<<< Updated upstream

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
=======
            addCabecalho(document, "👩‍🎓 Relatório de Alunos", null, null);

            List<AlunoModel> alunos = alunoService.buscarTodos();

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 5, 3, 3, 2});

            String[] headers = {"Matrícula", "Nome", "Curso", "Penalidade", "Qtd. Empréstimos"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                table.addCell(cell);
            }

            for (AlunoModel a : alunos) {
                table.addCell(a.getMatricula());
                table.addCell(a.getNomeCompleto());
                table.addCell(a.getCurso().getNome());
                table.addCell(a.getPenalidade() != null ? a.getPenalidade().toString() : "-");
                table.addCell(String.valueOf(a.getEmprestimosCount()));
            }

=======
            addCabecalho(document, "👩‍🎓 Relatório de Alunos", null, null);

            List<AlunoModel> alunos = alunoService.buscarTodos();

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 5, 3, 3, 2});

            String[] headers = {"Matrícula", "Nome", "Curso", "Penalidade", "Qtd. Empréstimos"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                table.addCell(cell);
            }

            for (AlunoModel a : alunos) {
                table.addCell(a.getMatricula());
                table.addCell(a.getNomeCompleto());
                table.addCell(a.getCurso().getNome());
                table.addCell(a.getPenalidade() != null ? a.getPenalidade().toString() : "-");
                table.addCell(String.valueOf(a.getEmprestimosCount()));
            }

>>>>>>> Stashed changes
            document.add(table);
            document.add(new Paragraph("\nTotal de alunos: " + alunos.size()));
        } catch (Exception ex) {
            throw new IOException("Erro ao gerar relatório de alunos", ex);
<<<<<<< Updated upstream
>>>>>>> Stashed changes
=======
>>>>>>> Stashed changes
        } finally {
            document.close();
        }
    }
<<<<<<< Updated upstream
<<<<<<< Updated upstream

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
=======

=======

>>>>>>> Stashed changes
    // 📖 Relatório de Livros
    public void gerarRelatorioLivros(OutputStream out) throws IOException {
        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            addCabecalho(document, "📖 Relatório de Livros", null, null);

            List<LivroModel> livros = livroService.buscarTodos();

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 4, 4, 2, 2, 3, 2});

            String[] headers = {"ISBN", "Título", "Autor", "CDD", "Classificação", "Gêneros", "Capa"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                table.addCell(cell);
            }

            for (LivroModel l : livros) {
                table.addCell(l.getIsbn());
                table.addCell(l.getNome());
                table.addCell(l.getAutor());
                table.addCell(l.getCdd().toString());
                table.addCell(l.getClassificacao_etaria().toString());
                table.addCell(l.getGeneros().isEmpty() ? "-" :
                        l.getGeneros().stream().map(g -> g.getNome()).collect(Collectors.joining(", ")));
                table.addCell(l.getTipo_capa() != null ? l.getTipo_capa().toString() : "-");
            }

            document.add(table);
            document.add(new Paragraph("\nTotal de livros: " + livros.size()));
        } catch (Exception ex) {
            throw new IOException("Erro ao gerar relatório de livros", ex);
<<<<<<< Updated upstream
=======
        } finally {
            document.close();
        }
    }

    // 🏷️ Relatório de Exemplares
    public void gerarRelatorioExemplares(OutputStream out) throws IOException {
        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            addCabecalho(document, "🏷️ Relatório de Exemplares", null, null);

            List<ExemplarModel> exemplares = exemplarService.buscarTodos();

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 5, 3, 4});

            String[] headers = {"Tombo", "Livro", "Status", "Localização"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                table.addCell(cell);
            }

            for (ExemplarModel e : exemplares) {
                table.addCell(e.getTombo());
                table.addCell(e.getLivro_isbn().getNome());
                table.addCell(e.getStatus_livro().toString());
                table.addCell(e.getLocalizacao_fisica());
            }

            document.add(table);
            document.add(new Paragraph("\nTotal de exemplares: " + exemplares.size()));
        } catch (Exception ex) {
            throw new IOException("Erro ao gerar relatório de exemplares", ex);
>>>>>>> Stashed changes
        } finally {
            document.close();
        }
    }

    // 🏷️ Relatório de Exemplares
    public void gerarRelatorioExemplares(OutputStream out) throws IOException {
        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            addCabecalho(document, "🏷️ Relatório de Exemplares", null, null);

            List<ExemplarModel> exemplares = exemplarService.buscarTodos();

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 5, 3, 4});

            String[] headers = {"Tombo", "Livro", "Status", "Localização"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                table.addCell(cell);
            }

            for (ExemplarModel e : exemplares) {
                table.addCell(e.getTombo());
                table.addCell(e.getLivro_isbn().getNome());
                table.addCell(e.getStatus_livro().toString());
                table.addCell(e.getLocalizacao_fisica());
            }

            document.add(table);
            document.add(new Paragraph("\nTotal de exemplares: " + exemplares.size()));
        } catch (Exception ex) {
            throw new IOException("Erro ao gerar relatório de exemplares", ex);
        } finally {
            document.close();
>>>>>>> Stashed changes
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