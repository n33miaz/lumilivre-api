package br.com.lumilivre.api.service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.lumilivre.api.enums.ClassificacaoEtaria;
import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.enums.TipoCapa;
import br.com.lumilivre.api.model.*;
import br.com.lumilivre.api.repository.*;
import br.com.lumilivre.api.utils.ExcelUtils;

@Service
public class ImportacaoService {

    private static final Logger log = LoggerFactory.getLogger(ImportacaoService.class);

    private final AlunoRepository alunoRepository;
    private final CursoRepository cursoRepository;
    private final LivroRepository livroRepository;
    private final ExemplarRepository exemplarRepository;
    private final GeneroRepository generoRepository;
    private final CddRepository cddRepository;

    private static final int BATCH_SIZE = 50;

    public ImportacaoService(
            AlunoRepository alunoRepository,
            CursoRepository cursoRepository,
            LivroRepository livroRepository,
            ExemplarRepository exemplarRepository,
            GeneroRepository generoRepository,
            CddRepository cddRepository) {
        this.alunoRepository = alunoRepository;
        this.cursoRepository = cursoRepository;
        this.livroRepository = livroRepository;
        this.exemplarRepository = exemplarRepository;
        this.generoRepository = generoRepository;
        this.cddRepository = cddRepository;
    }

    public String importar(String tipo, MultipartFile file) throws Exception {
        log.info("Iniciando importação do tipo: {}", tipo);
        try {
            validarArquivo(file);
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
        } catch (Exception e) {
            log.error("Erro durante importação do tipo {}: {}", tipo, e.getMessage(), e);
            throw new Exception("Falha na importação: " + e.getMessage(), e);
        }
    }

    // ============================= IMPORTAÇÃO DE ALUNOS
    // =============================
    @Transactional
    private String importarAlunos(MultipartFile file) throws Exception {
        List<AlunoModel> alunosParaSalvar = new ArrayList<>();
        List<ErroImportacao> logErros = new ArrayList<>();
        Set<String> matriculasNoExcel = new HashSet<>();

        Set<String> matriculasExistentes = alunoRepository.findAllMatriculas();
        Map<String, CursoModel> cursosMap = cursoRepository.findAll().stream()
                .collect(Collectors.toMap(
                        curso -> curso.getNome().toLowerCase().trim(),
                        curso -> curso,
                        (cursoExistente, novoCurso) -> cursoExistente));

        try (InputStream is = file.getInputStream();
                Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue;

                int linhaNum = row.getRowNum() + 1;
                String matricula = getCellString(row, 0);
                String nomeCompleto = getCellString(row, 1);
                String cursoNome = getCellString(row, 6);

                if (isBlank(matricula, nomeCompleto, cursoNome)) {
                    logErros.add(
                            new ErroImportacao(linhaNum, "Campos obrigatórios faltando (Matrícula, Nome ou Curso)"));
                    continue;
                }
                if (!matriculasNoExcel.add(matricula)) {
                    logErros.add(new ErroImportacao(linhaNum, "Matrícula duplicada no Excel: " + matricula));
                    continue;
                }
                if (matriculasExistentes.contains(matricula)) {
                    logErros.add(new ErroImportacao(linhaNum, "Aluno já existe no banco: " + matricula));
                    continue;
                }

                String cursoNomeNormalizado = cursoNome.toLowerCase().trim();
                CursoModel curso = cursosMap.get(cursoNomeNormalizado);
                if (curso == null) {
                    logErros.add(new ErroImportacao(linhaNum, "Curso não encontrado: " + cursoNome));
                    continue;
                }

                try {
                    AlunoModel aluno = criarAlunoFromRow(row, curso);
                    if (aluno.getCpf() != null && aluno.getCpf().isBlank()) {
                        aluno.setCpf(null);
                    }
                    if (validarAluno(aluno, linhaNum, logErros)) {
                        alunosParaSalvar.add(aluno);
                    }
                } catch (Exception e) {
                    logErros.add(new ErroImportacao(linhaNum, "Erro ao processar aluno: " + e.getMessage()));
                    log.error("Erro detalhado na linha {}: ", linhaNum, e);
                }
            }
        }
        return salvarAlunosEmLotes(alunosParaSalvar, logErros);
    }

    private AlunoModel criarAlunoFromRow(Row row, CursoModel curso) {
        AlunoModel aluno = new AlunoModel();
        aluno.setMatricula(getCellString(row, 0));
        aluno.setNomeCompleto(getCellString(row, 1));
        String cpfRaw = getCellString(row, 2);
        aluno.setCpf(cpfRaw == null || cpfRaw.isBlank() ? null : normalizeNumber(cpfRaw));
        aluno.setCelular(normalizeNumber(getCellString(row, 3)));
        aluno.setEmail(getCellString(row, 4));
        aluno.setDataNascimento(ExcelUtils.getLocalDate(row.getCell(5)));
        aluno.setCurso(curso);
        aluno.setCep(getCellString(row, 7));
        aluno.setLogradouro(getCellString(row, 8));
        aluno.setComplemento(getCellString(row, 9));
        aluno.setBairro(getCellString(row, 10));
        aluno.setLocalidade(getCellString(row, 11));
        aluno.setUf(getCellString(row, 12));
        aluno.setNumero_casa(getCellInteger(row, 13));
        aluno.setEmprestimosCount(0);
        return aluno;
    }

    private boolean validarAluno(AlunoModel aluno, int linhaNum, List<ErroImportacao> logErros) {
        boolean valido = true;
        if (aluno.getCpf() != null && !aluno.getCpf().isEmpty()) {
            if (aluno.getCpf().length() != 11) {
                logErros.add(new ErroImportacao(linhaNum, "CPF inválido: " + aluno.getCpf()));
                valido = false;
            } else {
                try {
                    if (alunoRepository.existsByCpf(aluno.getCpf())) {
                        logErros.add(new ErroImportacao(linhaNum, "CPF já cadastrado: " + aluno.getCpf()));
                        valido = false;
                    }
                } catch (Exception e) {
                    log.warn("Erro ao verificar CPF existente: {}", e.getMessage());
                }
            }
        }
        if (aluno.getEmail() != null && !aluno.getEmail().isEmpty() && !isEmailValido(aluno.getEmail())) {
            logErros.add(new ErroImportacao(linhaNum, "Email inválido: " + aluno.getEmail()));
            valido = false;
        }
        return valido;
    }

    private String salvarAlunosEmLotes(List<AlunoModel> alunos, List<ErroImportacao> logErros) {
        int totalSalvos = 0;
        for (int i = 0; i < alunos.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, alunos.size());
            List<AlunoModel> subLista = alunos.subList(i, end);
            try {
                alunoRepository.saveAll(subLista);
                totalSalvos += subLista.size();
                log.info("Lote de alunos {} a {} salvo com sucesso", i + 1, end);
            } catch (DataIntegrityViolationException e) {
                String rootCause = extrairRootCause(e);
                logErros.add(new ErroImportacao(-1,
                        "Erro de integridade no lote " + (i / BATCH_SIZE + 1) + ": " + rootCause));
                log.error("Erro de integridade no lote {}: {}", (i / BATCH_SIZE + 1), rootCause);
                break;
            } catch (Exception e) {
                String rootCause = extrairRootCause(e);
                logErros.add(
                        new ErroImportacao(-1, "Erro inesperado no lote " + (i / BATCH_SIZE + 1) + ": " + rootCause));
                log.error("Erro inesperado no lote {}: {}", (i / BATCH_SIZE + 1), rootCause, e);
            }
        }
        return gerarResumoImportacao("alunos", totalSalvos, logErros);
    }

    // ============================= IMPORTAÇÃO DE LIVROS
    // =============================
    @Transactional
    private String importarLivros(MultipartFile file) throws Exception {
        List<LivroModel> livrosParaSalvar = new ArrayList<>();
        List<ErroImportacao> logErros = new ArrayList<>();
        Set<String> isbnsNoExcel = new HashSet<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue;

                int linhaNum = row.getRowNum() + 1;
                try {
                    String isbn = getCellString(row, 1);
                    if (isbn != null && !isbn.isBlank()) {
                        if (!isbnsNoExcel.add(isbn)) {
                            logErros.add(new ErroImportacao(linhaNum, "ISBN duplicado no Excel: " + isbn));
                            continue;
                        }
                        if (livroRepository.findByIsbn(isbn).isPresent()) {
                            logErros.add(new ErroImportacao(linhaNum, "Livro com este ISBN já existe: " + isbn));
                            continue;
                        }
                    }

                    LivroModel livro = criarLivroFromRow(row);
                    livrosParaSalvar.add(livro);
                } catch (Exception e) {
                    logErros.add(new ErroImportacao(linhaNum, "Erro ao processar livro: " + e.getMessage()));
                }
            }
        }
        return salvarLivrosEmLotes(livrosParaSalvar, logErros);
    }

    private LivroModel criarLivroFromRow(Row row) {
        LivroModel livro = new LivroModel();
        livro.setIsbn(getCellString(row, 1));

        String cddCodigo = getCellString(row, 2);
        if (cddCodigo == null || cddCodigo.isBlank()) {
            throw new IllegalArgumentException("cdd_codigo (coluna 3) é obrigatório.");
        }
        CddModel cdd = cddRepository.findById(cddCodigo)
                .orElseThrow(() -> new IllegalArgumentException("CDD '" + cddCodigo + "' não encontrado no banco."));
        livro.setCdd(cdd);

        livro.setNome(getCellString(row, 4));
        livro.setAutor(getCellString(row, 5));
        livro.setEditora(getCellString(row, 6));
        livro.setData_lancamento(ExcelUtils.getLocalDate(row.getCell(7)));
        livro.setEdicao(getCellString(row, 8));
        livro.setNumero_paginas(getCellInteger(row, 10));
        livro.setClassificacao_etaria(
                ExcelUtils.getEnum(row.getCell(11), ClassificacaoEtaria.class, ClassificacaoEtaria.LIVRE));
        livro.setVolume(getCellInteger(row, 12));
        livro.setSinopse(getCellString(row, 13));
        livro.setTipo_capa(ExcelUtils.getEnum(row.getCell(14), TipoCapa.class, TipoCapa.BROCHURA));
        livro.setImagem(getCellString(row, 15));

        Set<GeneroModel> generos = generoRepository.findAllByCddCodigo(cddCodigo);
        livro.setGeneros(generos);

        return livro;
    }

    private String salvarLivrosEmLotes(List<LivroModel> livros, List<ErroImportacao> logErros) {
        int totalSalvos = 0;
        for (int i = 0; i < livros.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, livros.size());
            List<LivroModel> subLista = livros.subList(i, end);
            try {
                livroRepository.saveAll(subLista);
                totalSalvos += subLista.size();
                log.info("Lote de livros {} a {} salvo com sucesso", i + 1, end);
            } catch (Exception e) {
                String rootCause = extrairRootCause(e);
                logErros.add(new ErroImportacao(-1, "Erro no lote " + (i / BATCH_SIZE + 1) + ": " + rootCause));
                log.error("Erro ao salvar lote de livros {}: {}", (i / BATCH_SIZE + 1), rootCause, e);
            }
        }
        return gerarResumoImportacao("livros", totalSalvos, logErros);
    }

    // ============================= IMPORTAÇÃO DE EXEMPLARES
    // =============================
    @Transactional
    private String importarExemplares(MultipartFile file) throws Exception {
        List<ExemplarModel> exemplaresParaSalvar = new ArrayList<>();
        List<ErroImportacao> logErros = new ArrayList<>();
        Set<String> tombosNoExcel = new HashSet<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue;

                int linhaNum = row.getRowNum() + 1;
                try {
                    String tombo = getCellString(row, 1);
                    if (tombo == null || tombo.isBlank()) {
                        logErros.add(new ErroImportacao(linhaNum, "Tombo (coluna 2) é obrigatório."));
                        continue;
                    }
                    if (!tombosNoExcel.add(tombo)) {
                        logErros.add(new ErroImportacao(linhaNum, "Tombo duplicado no Excel: " + tombo));
                        continue;
                    }
                    if (exemplarRepository.existsById(tombo)) {
                        logErros.add(new ErroImportacao(linhaNum, "Exemplar com este tombo já existe: " + tombo));
                        continue;
                    }

                    ExemplarModel exemplar = criarExemplarFromRow(row);
                    exemplaresParaSalvar.add(exemplar);
                } catch (Exception e) {
                    logErros.add(new ErroImportacao(linhaNum, "Erro ao processar exemplar: " + e.getMessage()));
                }
            }
        }
        return salvarExemplaresEmLotes(exemplaresParaSalvar, logErros);
    }

    private ExemplarModel criarExemplarFromRow(Row row) {
        Long livroId = ExcelUtils.getLong(row.getCell(0));
        if (livroId == null) {
            throw new IllegalArgumentException("livro_id (coluna 1) é obrigatório.");
        }
        LivroModel livro = livroRepository.findById(livroId)
                .orElseThrow(
                        () -> new IllegalArgumentException("Livro com ID '" + livroId + "' não encontrado no banco."));

        ExemplarModel exemplar = new ExemplarModel();
        exemplar.setLivro(livro);
        exemplar.setTombo(getCellString(row, 1));
        exemplar.setStatus_livro(ExcelUtils.getEnum(row.getCell(2), StatusLivro.class, StatusLivro.DISPONIVEL));
        exemplar.setLocalizacao_fisica(getCellString(row, 3));

        return exemplar;
    }

    private String salvarExemplaresEmLotes(List<ExemplarModel> exemplares, List<ErroImportacao> logErros) {
        int totalSalvos = 0;
        for (int i = 0; i < exemplares.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, exemplares.size());
            List<ExemplarModel> subLista = exemplares.subList(i, end);
            try {
                exemplarRepository.saveAll(subLista);
                totalSalvos += subLista.size();
                log.info("Lote de exemplares {} a {} salvo com sucesso", i + 1, end);
            } catch (Exception e) {
                String rootCause = extrairRootCause(e);
                logErros.add(new ErroImportacao(-1, "Erro no lote " + (i / BATCH_SIZE + 1) + ": " + rootCause));
                log.error("Erro ao salvar lote de exemplares {}: {}", (i / BATCH_SIZE + 1), rootCause, e);
            }
        }

        if (totalSalvos > 0) {
            log.info("Atualizando contagem de exemplares nos livros...");
            Set<Long> livroIdsAfetados = exemplares.stream()
                    .map(e -> e.getLivro().getId())
                    .collect(Collectors.toSet());
            for (Long livroId : livroIdsAfetados) {
                Long contagem = exemplarRepository.countByLivroId(livroId);
                livroRepository.findById(livroId).ifPresent(livro -> {
                    livro.setQuantidade(contagem.intValue());
                    livroRepository.save(livro);
                });
            }
            log.info("Contagem de exemplares atualizada para {} livros.", livroIdsAfetados.size());
        }
        return gerarResumoImportacao("exemplares", totalSalvos, logErros);
    }

    // ============================= MÉTODOS AUXILIARES E CLASSE DE ERRO
    // =============================
    private void validarArquivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio ou nulo");
        }
        String contentType = file.getContentType();
        if (contentType == null
                || !contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            throw new IllegalArgumentException("Tipo de arquivo inválido. Use .xlsx");
        }
    }

    private String getCellString(Row row, int index) {
        return ExcelUtils.getString(row.getCell(index)).trim();
    }

    private Integer getCellInteger(Row row, int index) {
        return ExcelUtils.getInteger(row.getCell(index));
    }

    private boolean isBlank(String... values) {
        for (String v : values) {
            if (v == null || v.isBlank())
                return true;
        }
        return false;
    }

    private String normalizeNumber(String value) {
        if (value == null)
            return null;
        return value.replaceAll("\\D", "");
    }

    private boolean isEmailValido(String email) {
        if (email == null || email.isBlank())
            return false;
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private String extrairRootCause(Exception e) {
        Throwable rootCause = e;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause.getMessage();
    }

    private String gerarResumoImportacao(String tipo, int totalSalvos, List<ErroImportacao> logErros) {
        String resumo = String.format("✅ Importação de %s concluída. Salvos: %d | Erros: %d", tipo, totalSalvos,
                logErros.size());
        if (!logErros.isEmpty()) {
            String detalhes = logErros.stream()
                    .map(ErroImportacao::toString)
                    .limit(10)
                    .collect(Collectors.joining("; "));
            resumo += " | Primeiros erros: " + detalhes;
            if (logErros.size() > 10) {
                resumo += " ... (+" + (logErros.size() - 10) + " mais)";
            }
        }
        log.info(resumo);
        return resumo;
    }

    private static class ErroImportacao {
        private final int linha;
        private final String detalhe;

        public ErroImportacao(int linha, String detalhe) {
            this.linha = linha;
            this.detalhe = detalhe;
        }

        @Override
        public String toString() {
            return (linha > 0 ? "Linha " + linha + ": " : "") + detalhe;
        }
    }
}