package br.com.lumilivre.api.service;

import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.model.LivroModel;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.OutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class RelatorioService {

    private final EmprestimoService emprestimoService;
    private final AlunoService alunoService;
    private final LivroService livroService;

    public RelatorioService(EmprestimoService emprestimoService,
                            AlunoService alunoService,
                            LivroService livroService) {
        this.emprestimoService = emprestimoService;
        this.alunoService = alunoService;
        this.livroService = livroService;
    }

    public void gerarRelatorioEmprestimos(OutputStream out) throws IOException {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("📚 Relatório de Empréstimos"));
            document.add(new Paragraph(" "));

            List<EmprestimoModel> emprestimos = emprestimoService.buscarTodos();

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.addCell("ID");
            table.addCell("Aluno");
            table.addCell("Livro");
            table.addCell("Data");

            for (EmprestimoModel e : emprestimos) {
                table.addCell(String.valueOf(e.getId()));
                table.addCell(e.getAluno().getNomeCompleto());
                table.addCell(e.getExemplar().getLivro().getNome());
                table.addCell(e.getDataEmprestimo() != null ? e.getDataEmprestimo().toString() : "");
            }

            document.add(table);
        } catch (Exception ex) {
            throw new IOException("Erro gerando PDF de empréstimos", ex);
        } finally {
            document.close();
        }
    }

    public void gerarRelatorioAlunos(OutputStream out) throws IOException {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("👩‍🎓 Relatório de Alunos"));
            document.add(new Paragraph(" "));

            List<AlunoModel> alunos = alunoService.buscarTodos();

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.addCell("Matrícula");
            table.addCell("Nome");
            table.addCell("Quantidade de Empréstimos");

            for (AlunoModel a : alunos) {
                table.addCell(a.getMatricula());
                table.addCell(a.getNomeCompleto());
                table.addCell(String.valueOf(a.getEmprestimosCount()));
            }

            document.add(table);
        } catch (Exception ex) {
            throw new IOException("Erro gerando PDF de alunos", ex);
        } finally {
            document.close();
        }
    }
    
    public void gerarRelatorioEmprestimosCustomizado(OutputStream out, List<EmprestimoModel> emprestimos) throws IOException {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("📚 Relatório de Empréstimos"));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.addCell("ID");
            table.addCell("Aluno");
            table.addCell("Livro");
            table.addCell("Data");

            if (emprestimos != null) {
                for (EmprestimoModel e : emprestimos) {
                    table.addCell(String.valueOf(e.getId()));
                    table.addCell(e.getAluno().getNomeCompleto());
                    table.addCell(e.getExemplar().getLivro().getNome());
                    table.addCell(e.getDataEmprestimo() != null ? e.getDataEmprestimo().toString() : "");
                }
            }

            document.add(table);
        } catch (Exception ex) {
            throw new IOException("Erro gerando PDF de empréstimos", ex);
        } finally {
            document.close();
        }
    }
    
    public void gerarRelatorioEmprestimosAtivosEAtrasados(OutputStream out) throws IOException {
        List<EmprestimoModel> emprestimos = emprestimoService.buscarAtivosEAtrasados();
        gerarRelatorioEmprestimosCustomizado(out, emprestimos);
    }


    public void gerarRelatorioLivros(OutputStream out) throws IOException {
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("📖 Relatório de Livros"));
            document.add(new Paragraph(" "));

            List<LivroModel> livros = livroService.buscarTodos();

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.addCell("ISBN");
            table.addCell("Nome");
            table.addCell("Autor");
            table.addCell("Quantidade de Exemplares");

            for (LivroModel l : livros) {
                table.addCell(l.getIsbn());
                table.addCell(l.getNome());
                table.addCell(l.getAutor());
                table.addCell(String.valueOf(l.getQuantidade()));
            }

            document.add(table);
        } catch (Exception ex) {
            throw new IOException("Erro gerando PDF de livros", ex);
        } finally {
            document.close();
        }
    }
}
