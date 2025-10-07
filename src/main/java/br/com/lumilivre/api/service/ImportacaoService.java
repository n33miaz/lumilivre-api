package br.com.lumilivre.api.service;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import br.com.lumilivre.api.model.*;
import br.com.lumilivre.api.repository.*;
import br.com.lumilivre.api.utils.ExcelUtils;

@Service
public class ImportacaoService {

    private final AlunoRepository alunoRepository;
    private final CursoRepository cursoRepository;
    private final LivroRepository livroRepository;
    private final ExemplarRepository exemplarRepository;

    public ImportacaoService(
            AlunoRepository alunoRepository,
            CursoRepository cursoRepository,
            LivroRepository livroRepository,
            ExemplarRepository exemplarRepository) {
        this.alunoRepository = alunoRepository;
        this.cursoRepository = cursoRepository;
        this.livroRepository = livroRepository;
        this.exemplarRepository = exemplarRepository;
    }

    public String importar(String tipo, MultipartFile file) throws Exception {
        switch (tipo.toLowerCase()) {
            case "aluno":
                return importarAlunos(file);
            case "livro":
                return importarLivros(file);
            case "exemplar":
                return importarExemplares(file);
            default:
                throw new IllegalArgumentException("Tipo de importação inválido: " + tipo);
        }
    }

    // -------------------------------
    // 📘 Importação de Alunos
    // -------------------------------
    private String importarAlunos(MultipartFile file) throws Exception {
        List<AlunoModel> alunos = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            if (rows.hasNext()) rows.next(); // cabeçalho

            while (rows.hasNext()) {
                Row row = rows.next();

                String matricula = ExcelUtils.getString(row.getCell(0));
                if (matricula.isBlank()) continue;

                AlunoModel aluno = new AlunoModel();
                aluno.setMatricula(matricula);
                aluno.setNomeCompleto(ExcelUtils.getString(row.getCell(1)));
                aluno.setCpf(ExcelUtils.getString(row.getCell(2)));

                LocalDate nascimento = ExcelUtils.getLocalDate(row.getCell(3));
                if (nascimento != null) aluno.setDataNascimento(nascimento);

                aluno.setCelular(ExcelUtils.getString(row.getCell(4)));
                aluno.setEmail(ExcelUtils.getString(row.getCell(5)));

                String cursoNome = ExcelUtils.getString(row.getCell(6));
                cursoRepository.findByNomeIgnoreCase(cursoNome).ifPresent(aluno::setCurso);

                aluno.setEmprestimosCount(0);
                alunos.add(aluno);
            }
        }

        alunoRepository.saveAll(alunos);
        return "Importação de alunos concluída. Total: " + alunos.size();
    }

    // -------------------------------
    // 📚 Importação de Livros
    // -------------------------------
    private String importarLivros(MultipartFile file) throws Exception {
        List<LivroModel> livros = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            if (rows.hasNext()) rows.next(); // cabeçalho

            while (rows.hasNext()) {
                Row row = rows.next();
                String titulo = ExcelUtils.getString(row.getCell(0));
                if (titulo.isBlank()) continue;

                LivroModel livro = new LivroModel();
                livro.setNome(titulo);
                livro.setAutor(ExcelUtils.getString(row.getCell(1)));
                livro.setEditora(ExcelUtils.getString(row.getCell(2)));
                livro.setData_lancamento(ExcelUtils.getLocalDate(row.getCell(3)));
                livro.setIsbn(ExcelUtils.getString(row.getCell(4)));

                livros.add(livro);
            }
        }

        livroRepository.saveAll(livros);
        return "Importação de livros concluída. Total: " + livros.size();
    }

    // -------------------------------
    // 🏷️ Importação de Exemplares
    // -------------------------------
    private String importarExemplares(MultipartFile file) throws Exception {
        List<ExemplarModel> exemplares = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            if (rows.hasNext()) rows.next(); // cabeçalho

            while (rows.hasNext()) {
                Row row = rows.next();

                String tombo = ExcelUtils.getString(row.getCell(0));
                if (tombo.isBlank()) continue; // ignora linhas sem tombo

                ExemplarModel exemplar = new ExemplarModel();
                exemplar.setTombo(tombo);
                exemplar.setStatus_livro(null); // ou algum valor padrão

                String tituloLivro = ExcelUtils.getString(row.getCell(2));
                if (!tituloLivro.isBlank()) {
                    Optional<LivroModel> livroOpt = livroRepository.findByNomeIgnoreCase(tituloLivro);
                    livroOpt.ifPresent(livro -> exemplar.setLivro_isbn(livro));
                }


                exemplares.add(exemplar);
            }
        }

        exemplarRepository.saveAll(exemplares);
        return "Importação de exemplares concluída. Total: " + exemplares.size();
    }

}
