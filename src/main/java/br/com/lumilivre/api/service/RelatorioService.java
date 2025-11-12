package br.com.lumilivre.api.service;

import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.CddModel;
import br.com.lumilivre.api.model.CursoModel;
import br.com.lumilivre.api.model.EmprestimoModel;
import br.com.lumilivre.api.model.ExemplarModel;
import br.com.lumilivre.api.model.GeneroModel;
import br.com.lumilivre.api.model.LivroModel;
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

    private final EmprestimoService emprestimoService;
    private final AlunoService alunoService;
    private final LivroService livroService;
    private final ExemplarService exemplarService;
    private final CursoService cursoService;

    // --- CONSTANTES DE ESTILO ---
    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font FONT_CABECALHO_TABELA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
    private static final Font FONT_CORPO_TABELA = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Color COR_CABECALHO_TABELA = new Color(118, 32, 117); // Cor LumiLivre
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public RelatorioService(EmprestimoService emprestimoService,
                            AlunoService alunoService,
                            LivroService livroService,
                            ExemplarService exemplarService,
                            CursoService cursoService) {
        this.emprestimoService = emprestimoService;
        this.alunoService = alunoService;
        this.livroService = livroService;
        this.exemplarService = exemplarService;
        this.cursoService = cursoService;
    }

    // -----------------------
    // EMPRÉSTIMOS
    // -----------------------

    /**
     * Relatório de empréstimos com filtros opcionais: data, status, aluno, curso, isbn/tombo, modulo.
     */
    public void gerarRelatorioEmprestimosPorFiltros(OutputStream out,
                                                    LocalDate inicio,
                                                    LocalDate fim,
                                                    StatusEmprestimo status,
                                                    Long idAluno,
                                                    Long idCurso,
                                                    String isbnOuTombo,
                                                    String modulo) throws IOException {
        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "📘 Relatório de Empréstimos (Filtrado)", inicio, fim);

            List<EmprestimoModel> emprestimos = emprestimoService.buscarTodos().stream()
            	    .filter(e -> (inicio == null || !e.getDataEmprestimo().toLocalDate().isBefore(inicio)))
            	    .filter(e -> (fim == null || !e.getDataEmprestimo().toLocalDate().isAfter(fim)))
            	    .filter(e -> (status == null || e.getStatusEmprestimo() == status))
            	    .filter(e -> (idAluno == null || (e.getAluno() != null && Objects.equals(e.getAluno().getMatricula(), idAluno))))
            	    .filter(e -> (idCurso == null || (e.getAluno() != null && e.getAluno().getCurso() != null
            	            && Objects.equals(e.getAluno().getCurso().getId(), idCurso))))
            	    .filter(e -> (isbnOuTombo == null || isbnOuTombo.isBlank() || (
            	            e.getExemplar() != null && e.getExemplar().getLivro() != null &&
            	                    (isbnOuTombo.equalsIgnoreCase(e.getExemplar().getTombo())
            	                            || isbnOuTombo.equalsIgnoreCase(e.getExemplar().getLivro().getIsbn()))
            	    )))
            	    .filter(e -> (modulo == null || modulo.isBlank() ||
            	        modulo.equalsIgnoreCase(
            	            Optional.ofNullable(e.getAluno())
            	                    .map(a -> Optional.ofNullable(a.getCurso()).map(c -> c.getModulo()).orElse(""))
            	                    .orElse("")
            	        )
            	    ))
            	    .collect(Collectors.toList());


            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.2f, 3.5f, 3f, 2.5f, 4f, 2.5f, 2f});

            adicionarCelulaHeader(table, "ID");
            adicionarCelulaHeader(table, "Aluno");
            adicionarCelulaHeader(table, "Curso");
            adicionarCelulaHeader(table, "Módulo");
            adicionarCelulaHeader(table, "Livro / Tombo");
            adicionarCelulaHeader(table, "Data Empréstimo");
            adicionarCelulaHeader(table, "Status");

            for (EmprestimoModel e : emprestimos) {
                table.addCell(criarCelulaDados(String.valueOf(e.getId())));

                // Nome do aluno
                table.addCell(criarCelulaDados(
                    Optional.ofNullable(e.getAluno())
                            .map(AlunoModel::getNomeCompleto)
                            .orElse("N/A")
                ));

                // Nome do curso
                table.addCell(criarCelulaDados(
                    Optional.ofNullable(e.getAluno())
                            .map(a -> Optional.ofNullable(a.getCurso())
                                    .map(CursoModel::getNome)
                                    .orElse("N/A"))
                            .orElse("N/A")
                ));

                // Módulo do curso
                table.addCell(criarCelulaDados(
                    Optional.ofNullable(e.getAluno())
                            .map(a -> Optional.ofNullable(a.getCurso())
                                    .map(c -> Optional.ofNullable(c.getModulo()).orElse("-"))
                                    .orElse("-"))
                            .orElse("-")
                ));

                // Livro (ou tombo se não tiver livro)
                String livroTombo = Optional.ofNullable(e.getExemplar())
                        .map(ex -> Optional.ofNullable(ex.getLivro())
                                .map(LivroModel::getNome)
                                .orElse(ex.getTombo()))
                        .orElse("N/A");
                table.addCell(criarCelulaDados(livroTombo));

                // Data e status
                table.addCell(criarCelulaDados(formatarData(e.getDataEmprestimo())));
                table.addCell(criarCelulaDados(
                    e.getStatusEmprestimo() != null ? e.getStatusEmprestimo().name() : "-"
                ));
            }

            document.add(table);
            adicionarRodapeRelatorio(document, "Total de empréstimos filtrados: " + emprestimos.size());

        } catch (Exception ex) {
            log.error("Erro ao gerar relatório de empréstimos filtrados", ex);
            throw new IOException("Erro ao gerar relatório de empréstimos filtrados", ex);
        } finally {
            document.close();
        }
    }

    /**
     * Contagem de empréstimos por status (estatístico).
     */
    public void gerarContagemEmprestimosPorStatus(OutputStream out) throws IOException {
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "📊 Contagem de Empréstimos por Status", null, null);

            List<EmprestimoModel> emprestimos = emprestimoService.buscarTodos();

            Map<StatusEmprestimo, Long> contagem = emprestimos.stream()
                    .collect(Collectors.groupingBy(EmprestimoModel::getStatusEmprestimo, Collectors.counting()));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(50);
            table.setHorizontalAlignment(Element.ALIGN_CENTER);

            adicionarCelulaHeader(table, "Status");
            adicionarCelulaHeader(table, "Quantidade");

            // Ordem consistente de enum (se aplicável)
            Arrays.stream(StatusEmprestimo.values()).forEach(s -> {
                Long qtd = contagem.getOrDefault(s, 0L);
                table.addCell(criarCelulaDados(s.name()));
                table.addCell(criarCelulaDados(String.valueOf(qtd)));
            });

            document.add(table);
            adicionarRodapeRelatorio(document, "Total geral: " + emprestimos.size());

        } catch (Exception ex) {
            log.error("Erro ao gerar contagem de empréstimos por status", ex);
            throw new IOException("Erro ao gerar contagem de empréstimos por status", ex);
        } finally {
            document.close();
        }
    }

    // -----------------------
    // ALUNOS
    // -----------------------

    /**
     * Relatório de alunos com filtros: modulo, curso, turno, penalidade.
     * Também traz a contagem de empréstimos por aluno (se disponível).
     */
    public void gerarRelatorioAlunosPorFiltros(OutputStream out,
                                               String modulo,
                                               Long idCurso,
                                               String turno,
                                               Penalidade penalidade) throws IOException {
        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "👩‍🎓 Relatório de Alunos (Filtrado)", null, null);

            List<AlunoModel> alunos = alunoService.buscarTodos().stream()
                    // Filtro por módulo (que vem do curso)
                    .filter(a -> (modulo == null || modulo.isBlank() ||
                            modulo.equalsIgnoreCase(
                                    Optional.ofNullable(a.getCurso())
                                            .map(CursoModel::getModulo)
                                            .orElse("")
                            )))
                    // Filtro por curso (id)
                    .filter(a -> (idCurso == null ||
                            (a.getCurso() != null && Objects.equals(a.getCurso().getId(), idCurso))))
                    // Filtro por turno (enum do curso)
                    .filter(a -> (turno == null || turno.isBlank() ||
                            turno.equalsIgnoreCase(
                                    Optional.ofNullable(a.getCurso())
                                            .map(CursoModel::getTurno)
                                            .map(Enum::name) // transforma o enum em String
                                            .orElse("")
                            )))
                    // Filtro por penalidade
                    .filter(a -> (penalidade == null || penalidade.equals(a.getPenalidade())))
                    .collect(Collectors.toList());




            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 5f, 3f, 2f, 2f, 2f});

            adicionarCelulaHeader(table, "Matrícula");
            adicionarCelulaHeader(table, "Nome");
            adicionarCelulaHeader(table, "Curso");
            adicionarCelulaHeader(table, "Módulo");
            adicionarCelulaHeader(table, "Penalidade");
            adicionarCelulaHeader(table, "Qtd. Empréstimos");

            for (AlunoModel a : alunos) {
                table.addCell(criarCelulaDados(Optional.ofNullable(a.getMatricula()).orElse("-")));
                table.addCell(criarCelulaDados(Optional.ofNullable(a.getNomeCompleto()).orElse("-")));
                table.addCell(criarCelulaDados(Optional.ofNullable(a.getCurso()).map(CursoModel::getNome).orElse("N/A")));
                table.addCell(criarCelulaDados(
                        Optional.ofNullable(a.getCurso())
                                .map(CursoModel::getModulo)
                                .orElse("-")
                ));
                table.addCell(criarCelulaDados(a.getPenalidade() != null ? a.getPenalidade().name() : "-"));
                table.addCell(criarCelulaDados(String.valueOf(obterContagemEmprestimos(a))));
            }

            document.add(table);
            adicionarRodapeRelatorio(document, "Total de alunos: " + alunos.size());

        } catch (Exception ex) {
            log.error("Erro ao gerar relatório de alunos filtrados", ex);
            throw new IOException("Erro ao gerar relatório de alunos filtrados", ex);
        } finally {
            document.close();
        }
    }

    /**
     * Relatório com contagem de empréstimos por aluno (top N opcional).
     */
    public void gerarRelatorioContagemEmprestimosPorAluno(OutputStream out, Integer topN) throws IOException {
        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "📈 Empréstimos por Aluno", null, null);

            List<AlunoModel> alunos = alunoService.buscarTodos();

            // Map aluno -> contagem
            List<Map.Entry<AlunoModel, Long>> lista = alunos.stream()
                    .map(a -> new AbstractMap.SimpleEntry<>(a, obterContagemEmprestimos(a)))
                    .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                    .collect(Collectors.toList());

            if (topN != null && topN > 0) {
                lista = lista.stream().limit(topN).collect(Collectors.toList());
            }

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(80);
            table.setWidths(new float[]{3f, 5f, 2f});

            adicionarCelulaHeader(table, "Matrícula");
            adicionarCelulaHeader(table, "Nome");
            adicionarCelulaHeader(table, "Qtd. Empréstimos");

            for (Map.Entry<AlunoModel, Long> entry : lista) {
                AlunoModel a = entry.getKey();
                table.addCell(criarCelulaDados(Optional.ofNullable(a.getMatricula()).orElse("-")));
                table.addCell(criarCelulaDados(Optional.ofNullable(a.getNomeCompleto()).orElse("-")));
                table.addCell(criarCelulaDados(String.valueOf(entry.getValue())));
            }

            document.add(table);
            adicionarRodapeRelatorio(document, "Total estudantes considerados: " + alunos.size());

        } catch (Exception ex) {
            log.error("Erro ao gerar relatório de contagem de empréstimos por aluno", ex);
            throw new IOException("Erro ao gerar relatório de contagem de empréstimos por aluno", ex);
        } finally {
            document.close();
        }
    }

    // -----------------------
    // LIVROS
    // -----------------------

    /**
     * Relatório geral de livros (total) e filtros por genero, autor, cdd, classificacao, tipo capa.
     */
    public void gerarRelatorioLivrosFiltrados(OutputStream out,
                                              String genero,
                                              String autor,
                                              String cdd,
                                              String classificacaoEtaria,
                                              String tipoCapa) throws IOException {
        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "📖 Relatório de Livros (Filtrado)", null, null);

            List<LivroModel> livros = livroService.buscarTodos().stream()
                    // 🔹 Filtro por gênero
                    .filter(l -> genero == null || genero.isBlank() ||
                            Optional.ofNullable(l.getGeneros())
                                    .orElse(Collections.emptySet())
                                    .stream()
                                    .map(GeneroModel::getNome)
                                    .anyMatch(g -> g != null && g.equalsIgnoreCase(genero))
                    )
                    // 🔹 Filtro por autor
                    .filter(l -> autor == null || autor.isBlank() ||
                            (l.getAutor() != null && l.getAutor().toLowerCase().contains(autor.toLowerCase()))
                    )
                    // 🔹 Filtro por CDD (ajustado conforme seu modelo)
                    .filter(l -> cdd == null || cdd.isBlank() ||
                            (l.getCdd() != null && (
                                    (l.getCdd().getCodigo() != null && l.getCdd().getCodigo().equalsIgnoreCase(cdd)) ||
                                    (l.getCdd().getDescricao() != null && l.getCdd().getDescricao().equalsIgnoreCase(cdd))
                            ))
                    )
                    // 🔹 Filtro por classificação etária (enum)
                    .filter(l -> classificacaoEtaria == null || classificacaoEtaria.isBlank() ||
                            (l.getClassificacao_etaria() != null &&
                                    l.getClassificacao_etaria().name().equalsIgnoreCase(classificacaoEtaria))
                    )
                    // 🔹 Filtro por tipo de capa (enum)
                    .filter(l -> tipoCapa == null || tipoCapa.isBlank() ||
                            (l.getTipo_capa() != null &&
                                    l.getTipo_capa().name().equalsIgnoreCase(tipoCapa))
                    )
                    .collect(Collectors.toList());




            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.2f, 4f, 3f, 3f, 2f, 2f});

            adicionarCelulaHeader(table, "ID");
            adicionarCelulaHeader(table, "Título");
            adicionarCelulaHeader(table, "Autor");
            adicionarCelulaHeader(table, "Gêneros");
            adicionarCelulaHeader(table, "CDD");
            adicionarCelulaHeader(table, "Qtd. Exemplares");

            for (LivroModel l : livros) {
                table.addCell(criarCelulaDados(String.valueOf(l.getId())));
                table.addCell(criarCelulaDados(Optional.ofNullable(l.getNome()).orElse("-")));
                table.addCell(criarCelulaDados(Optional.ofNullable(l.getAutor()).orElse("-")));
                String generos = (l.getGeneros() == null || l.getGeneros().isEmpty()) ? "-"
                        : l.getGeneros().stream().map(g -> g.getNome()).collect(Collectors.joining(", "));
                table.addCell(criarCelulaDados(generos));
                table.addCell(criarCelulaDados(
                	    Optional.ofNullable(l.getCdd())
                	        .map(CddModel::getCodigo)
                	        .orElse("-")
                	));
                table.addCell(criarCelulaDados(String.valueOf(Optional.ofNullable(l.getQuantidade()).orElse(0))));
            }

            document.add(table);
            adicionarRodapeRelatorio(document, "Total de títulos: " + livros.size());

        } catch (Exception ex) {
            log.error("Erro ao gerar relatório de livros filtrados", ex);
            throw new IOException("Erro ao gerar relatório de livros filtrados", ex);
        } finally {
            document.close();
        }
    }

    /**
     * Relatório com quantidade total e agregados (por genero, autor).
     */
    public void gerarRelatorioEstatisticasLivros(OutputStream out) throws IOException {
        Document document = new Document(PageSize.A4);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "📊 Estatísticas de Livros", null, null);

            List<LivroModel> livros = livroService.buscarTodos();

            long total = livros.size();
            Map<String, Long> porAutor = livros.stream().collect(Collectors.groupingBy(
                    l -> Optional.ofNullable(l.getAutor()).orElse("Desconhecido"), Collectors.counting()));
            Map<String, Long> porGenero = livros.stream()
                    .flatMap(l -> Optional.ofNullable(l.getGeneros())
                            .orElse(Collections.<GeneroModel>emptySet()) // garante tipo correto
                            .stream()
                            .map(GeneroModel::getNome))
                    .collect(Collectors.groupingBy(g -> g, Collectors.counting()));


            // Tabela resumo
            PdfPTable tableResumo = new PdfPTable(2);
            tableResumo.setWidthPercentage(40);
            adicionarCelulaHeader(tableResumo, "Métrica");
            adicionarCelulaHeader(tableResumo, "Valor");
            tableResumo.addCell(criarCelulaDados("Total títulos"));
            tableResumo.addCell(criarCelulaDados(String.valueOf(total)));

            document.add(tableResumo);
            document.add(Chunk.NEWLINE);

            // Por autor (top 10)
            PdfPTable tAutor = new PdfPTable(2);
            tAutor.setWidthPercentage(60);
            adicionarCelulaHeader(tAutor, "Autor");
            adicionarCelulaHeader(tAutor, "Quantidade");
            porAutor.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(10)
                    .forEach(e -> {
                        tAutor.addCell(criarCelulaDados(e.getKey()));
                        tAutor.addCell(criarCelulaDados(String.valueOf(e.getValue())));
                    });
            document.add(tAutor);
            document.add(Chunk.NEWLINE);

            // Por genero
            PdfPTable tGenero = new PdfPTable(2);
            tGenero.setWidthPercentage(60);
            adicionarCelulaHeader(tGenero, "Gênero");
            adicionarCelulaHeader(tGenero, "Quantidade");
            porGenero.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(e -> {
                        tGenero.addCell(criarCelulaDados(e.getKey()));
                        tGenero.addCell(criarCelulaDados(String.valueOf(e.getValue())));
                    });
            document.add(tGenero);

        } catch (Exception ex) {
            log.error("Erro ao gerar estatísticas de livros", ex);
            throw new IOException("Erro ao gerar estatísticas de livros", ex);
        } finally {
            document.close();
        }
    }

    // -----------------------
    // EXEMPLARES
    // -----------------------

    /**
     * Relatório de exemplares filtrado por status ou por isbn/tombo.
     */
    public void gerarRelatorioExemplaresFiltrados(OutputStream out, StatusLivro status, String isbnOuTombo) throws IOException {
        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "🏷️ Relatório de Exemplares (Filtrado)", null, null);

            List<ExemplarModel> exemplares = exemplarService.buscarTodos().stream()
                    .filter(ex -> (status == null || ex.getStatus_livro() == status))
                    .filter(ex -> (isbnOuTombo == null || isbnOuTombo.isBlank() || isbnOuTombo.equalsIgnoreCase(ex.getTombo())
                            || (ex.getLivro() != null && isbnOuTombo.equalsIgnoreCase(ex.getLivro().getIsbn()))))
                    .collect(Collectors.toList());

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 4f, 2.5f, 3f, 3f});

            adicionarCelulaHeader(table, "Tombo");
            adicionarCelulaHeader(table, "Título");
            adicionarCelulaHeader(table, "Status");
            adicionarCelulaHeader(table, "Localização");
            adicionarCelulaHeader(table, "ISBN");

            for (ExemplarModel e : exemplares) {
                table.addCell(criarCelulaDados(Optional.ofNullable(e.getTombo()).orElse("-")));
                table.addCell(criarCelulaDados(Optional.ofNullable(e.getLivro()).map(LivroModel::getNome).orElse("N/A")));
                table.addCell(criarCelulaDados(Optional.ofNullable(e.getStatus_livro()).map(Enum::name).orElse("-")));
                table.addCell(criarCelulaDados(Optional.ofNullable(e.getLocalizacao_fisica()).orElse("-")));
                table.addCell(criarCelulaDados(Optional.ofNullable(e.getLivro()).map(LivroModel::getIsbn).orElse("-")));
            }

            document.add(table);
            adicionarRodapeRelatorio(document, "Total de exemplares: " + exemplares.size());

        } catch (Exception ex) {
            log.error("Erro ao gerar relatório de exemplares filtrados", ex);
            throw new IOException("Erro ao gerar relatório de exemplares filtrados", ex);
        } finally {
            document.close();
        }
    }

    // -----------------------
    // CURSOS
    // -----------------------

    /**
     * Relatório geral de cursos com total de alunos e total de empréstimos por curso.
     */
    public void gerarRelatorioCursosGeral(OutputStream out) throws IOException {
        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            adicionarCabecalhoRelatorio(document, "🏫 Relatório de Cursos (Geral)", null, null);

            List<CursoModel> cursos = cursoService.buscarTodos();

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 2f, 2f, 2f});

            adicionarCelulaHeader(table, "Curso");
            adicionarCelulaHeader(table, "Qtd. Alunos");
            adicionarCelulaHeader(table, "Qtd. Empréstimos");
            adicionarCelulaHeader(table, "Média Empréstimos/Aluno");

            for (CursoModel c : cursos) {
                List<AlunoModel> alunosDoCurso = Optional.ofNullable(c.getAlunos()).orElse(Collections.emptyList());
                long qtdAlunos = alunosDoCurso.size();
                long qtdEmprestimos = alunosDoCurso.stream()
                        .mapToLong(a -> obterContagemEmprestimos(a))
                        .sum();
                double media = qtdAlunos == 0 ? 0 : ((double) qtdEmprestimos / qtdAlunos);
                table.addCell(criarCelulaDados(Optional.ofNullable(c.getNome()).orElse("-")));
                table.addCell(criarCelulaDados(String.valueOf(qtdAlunos)));
                table.addCell(criarCelulaDados(String.valueOf(qtdEmprestimos)));
                table.addCell(criarCelulaDados(String.format("%.2f", media)));
            }

            document.add(table);
            adicionarRodapeRelatorio(document, "Total de cursos: " + cursos.size());

        } catch (Exception ex) {
            log.error("Erro ao gerar relatório geral de cursos", ex);
            throw new IOException("Erro ao gerar relatório geral de cursos", ex);
        } finally {
            document.close();
        }
    }

    // -----------------------
    // HELPERS
    // -----------------------
    private long obterContagemEmprestimos(AlunoModel a) {
        if (a == null) {
            return 0;
        }
        // Retorna o campo emprestimosCount, garantindo não ser null
        return a.getEmprestimosCount();
    }



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
