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

import br.com.lumilivre.api.enums.Cdd;
import br.com.lumilivre.api.enums.StatusLivro;
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
    
    private static final int BATCH_SIZE = 50;

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


 // ==========================================================
 // 📘 IMPORTAÇÃO DE ALUNOS - CORRIGIDO (CPFs vazios tratados)
 // ==========================================================
 @Transactional
 private String importarAlunos(MultipartFile file) throws Exception {
     List<AlunoModel> alunosParaSalvar = new ArrayList<>();
     List<ErroImportacao> logErros = new ArrayList<>();
     Set<String> matriculasNoExcel = new HashSet<>();

     // Buscar todas as matrículas existentes
     Set<String> matriculasExistentes = alunoRepository.findAllMatriculas();

     // Buscar cursos e criar mapa case-insensitive
     Map<String, CursoModel> cursosMap = new HashMap<>();
     for (CursoModel curso : cursoRepository.findAll()) {
         String nomeNormalizado = curso.getNome().toLowerCase().trim();
         cursosMap.putIfAbsent(nomeNormalizado, curso); // Evita duplicatas
     }

     try (InputStream is = file.getInputStream();
          Workbook workbook = WorkbookFactory.create(is)) {

         Sheet sheet = workbook.getSheetAt(0);
         for (Row row : sheet) {
             if (row.getRowNum() == 0) continue;

             int linhaNum = row.getRowNum() + 1;
             String matricula = getCellString(row, 0);
             String nomeCompleto = getCellString(row, 1);
             String cursoNome = getCellString(row, 6);

             // Campos obrigatórios
             if (isBlank(matricula, nomeCompleto, cursoNome)) {
                 logErros.add(new ErroImportacao(linhaNum, "Campos obrigatórios faltando (Matrícula, Nome ou Curso)"));
                 continue;
             }

             // Duplicata no Excel
             if (!matriculasNoExcel.add(matricula)) {
                 logErros.add(new ErroImportacao(linhaNum, "Matrícula duplicada no Excel: " + matricula));
                 continue;
             }

             // Duplicata no banco
             if (matriculasExistentes.contains(matricula)) {
                 logErros.add(new ErroImportacao(linhaNum, "Aluno já existe no banco: " + matricula));
                 continue;
             }

             // Validação do curso
             String cursoNomeNormalizado = cursoNome.toLowerCase().trim();
             CursoModel curso = cursosMap.get(cursoNomeNormalizado);
             if (curso == null) {
                 logErros.add(new ErroImportacao(linhaNum, "Curso não encontrado: " + cursoNome));
                 continue;
             }

             try {
                 // Criar e validar aluno
                 AlunoModel aluno = criarAlunoFromRow(row, curso);

                 // Tratar CPF vazio (null ao invés de "")
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

     // Salvar em lotes com transação
     return salvarAlunosEmLotes(alunosParaSalvar, logErros);
 }

 private AlunoModel criarAlunoFromRow(Row row, CursoModel curso) {
     AlunoModel aluno = new AlunoModel();
     aluno.setMatricula(getCellString(row, 0));
     aluno.setNomeCompleto(getCellString(row, 1));

     // Corrigido: CPF vazio -> null
     String cpfRaw = getCellString(row, 2);
     aluno.setCpf(cpfRaw == null || cpfRaw.isBlank() ? null : normalizeNumber(cpfRaw));

     aluno.setCelular(normalizeNumber(getCellString(row, 3)));
     aluno.setEmail(getCellString(row, 4));
     aluno.setDataNascimento(ExcelUtils.getLocalDate(row.getCell(5)));
     aluno.setCurso(curso); // Usa o curso do mapa (mesma instância)
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

        // Validar CPF se presente
        if (aluno.getCpf() != null && !aluno.getCpf().isEmpty()) {
            if (aluno.getCpf().length() != 11) {
                logErros.add(new ErroImportacao(linhaNum, "CPF inválido: " + aluno.getCpf()));
                valido = false;
            } else {
                // Verificar se CPF já existe (apenas se CPF for válido)
                try {
                    if (alunoRepository.existsByCpf(aluno.getCpf())) {
                        logErros.add(new ErroImportacao(linhaNum, "CPF já cadastrado: " + aluno.getCpf()));
                        valido = false;
                    }
                } catch (Exception e) {
                    log.warn("Erro ao verificar CPF existente: {}", e.getMessage());
                    // Continua mesmo com erro na verificação do CPF
                }
            }
        }

        // Validar email se presente
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
                // Validar cada aluno antes de salvar
                for (AlunoModel aluno : subLista) {
                    if (!isAlunoValidoParaSalvar(aluno)) {
                        throw new DataIntegrityViolationException("Dados inválidos encontrados no lote");
                    }
                }
                
                alunoRepository.saveAll(subLista);
                totalSalvos += subLista.size();
                log.info("Lote de alunos {} a {} salvo com sucesso", i + 1, end);
                
            } catch (DataIntegrityViolationException e) {
                String rootCause = extrairRootCause(e);
                logErros.add(new ErroImportacao(-1, "Erro de integridade no lote " + (i/BATCH_SIZE + 1) + ": " + rootCause));
                log.error("Erro de integridade no lote {}: {}", (i/BATCH_SIZE + 1), rootCause);
                
                // Rollback automático devido ao @Transactional
                break; // Para de processar lotes em caso de erro de integridade
                
            } catch (Exception e) {
                String rootCause = extrairRootCause(e);
                logErros.add(new ErroImportacao(-1, "Erro inesperado no lote " + (i/BATCH_SIZE + 1) + ": " + rootCause));
                log.error("Erro inesperado no lote {}: {}", (i/BATCH_SIZE + 1), rootCause, e);
            }
        }

        return gerarResumoImportacao("alunos", totalSalvos, logErros);
    }

    // ==========================================================
    // 📚 IMPORTAÇÃO DE LIVROS
    // ==========================================================
    @Transactional
    private String importarLivros(MultipartFile file) throws Exception {
        List<LivroModel> livrosParaSalvar = new ArrayList<>();
        List<ErroImportacao> logErros = new ArrayList<>();
        Set<String> isbnsNoExcel = new HashSet<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                int linhaNum = row.getRowNum() + 1;
                String isbn = getCellString(row, 0);

                if (isBlank(isbn)) {
                    logErros.add(new ErroImportacao(linhaNum, "ISBN vazio"));
                    continue;
                }

                if (!isbnsNoExcel.add(isbn)) {
                    logErros.add(new ErroImportacao(linhaNum, "ISBN duplicado no Excel: " + isbn));
                    continue;
                }

                if (livroRepository.existsByIsbn(isbn)) {
                    logErros.add(new ErroImportacao(linhaNum, "Livro já existe: " + isbn));
                    continue;
                }

                try {
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
        livro.setIsbn(getCellString(row, 0));
        livro.setNome(getCellString(row, 1));
        livro.setAutor(getCellString(row, 2));
        livro.setEditora(getCellString(row, 3));
        livro.setData_lancamento(ExcelUtils.getLocalDate(row.getCell(4)));
        livro.setNumero_paginas(getCellInteger(row, 5));

        String cddNome = getCellString(row, 6);
        if (!cddNome.isBlank()) {
            try {
                livro.setCdd(Cdd.searchByPartialDescription(cddNome));
            } catch (IllegalArgumentException e) {
                livro.setCdd(null);
            }
        }

        livro.setQuantidade(getCellInteger(row, 7));
        livro.setGenero(getCellString(row, 8));

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
                logErros.add(new ErroImportacao(-1, "Erro no lote " + (i/BATCH_SIZE + 1) + ": " + rootCause));
                log.error("Erro ao salvar lote de livros {}: {}", (i/BATCH_SIZE + 1), rootCause, e);
            }
        }

        return gerarResumoImportacao("livros", totalSalvos, logErros);
    }

    // ==========================================================
    // 🏷️ IMPORTAÇÃO DE EXEMPLARES
    // ==========================================================
    @Transactional
    private String importarExemplares(MultipartFile file) throws Exception {
        List<ExemplarModel> exemplaresParaSalvar = new ArrayList<>();
        List<ErroImportacao> logErros = new ArrayList<>();
        Set<String> tombosNoExcel = new HashSet<>();

        // Buscar todos os ISBNs de uma vez para otimização
        Map<String, LivroModel> livrosMap = livroRepository.findAll().stream()
                .collect(Collectors.toMap(LivroModel::getIsbn, livro -> livro));

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                int linhaNum = row.getRowNum() + 1;
                String tombo = getCellString(row, 0);

                if (tombo.isBlank()) {
                    logErros.add(new ErroImportacao(linhaNum, "Tombo vazio"));
                    continue;
                }

                if (!tombosNoExcel.add(tombo)) {
                    logErros.add(new ErroImportacao(linhaNum, "Tombo duplicado no Excel: " + tombo));
                    continue;
                }

                if (exemplarRepository.existsByTombo(tombo)) {
                    logErros.add(new ErroImportacao(linhaNum, "Exemplar já existe: " + tombo));
                    continue;
                }

                try {
                    ExemplarModel exemplar = criarExemplarFromRow(row, livrosMap, linhaNum, logErros);
                    if (exemplar != null) {
                        exemplaresParaSalvar.add(exemplar);
                    }
                    
                } catch (Exception e) {
                    logErros.add(new ErroImportacao(linhaNum, "Erro ao processar exemplar: " + e.getMessage()));
                }
            }
        }

        return salvarExemplaresEmLotes(exemplaresParaSalvar, logErros);
    }

    private ExemplarModel criarExemplarFromRow(Row row, Map<String, LivroModel> livrosMap, int linhaNum, List<ErroImportacao> logErros) {
        ExemplarModel exemplar = new ExemplarModel();
        exemplar.setTombo(getCellString(row, 0));

        String statusStr = getCellString(row, 1);
        if (!statusStr.isBlank()) {
            try {
                exemplar.setStatus_livro(StatusLivro.valueOf(statusStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                logErros.add(new ErroImportacao(linhaNum, "Status inválido: " + statusStr));
                exemplar.setStatus_livro(StatusLivro.DISPONIVEL); // Valor padrão
            }
        } else {
            exemplar.setStatus_livro(StatusLivro.DISPONIVEL); // Valor padrão
        }

        String isbnLivro = getCellString(row, 2);
        if (!isbnLivro.isBlank()) {
            LivroModel livro = livrosMap.get(isbnLivro);
            if (livro != null) {
                exemplar.setLivro_isbn(livro);
            } else {
                logErros.add(new ErroImportacao(linhaNum, "Livro não encontrado: " + isbnLivro));
                return null; // Não salvar exemplar sem livro válido
            }
        } else {
            logErros.add(new ErroImportacao(linhaNum, "ISBN do livro é obrigatório"));
            return null;
        }

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
                logErros.add(new ErroImportacao(-1, "Erro no lote " + (i/BATCH_SIZE + 1) + ": " + rootCause));
                log.error("Erro ao salvar lote de exemplares {}: {}", (i/BATCH_SIZE + 1), rootCause, e);
            }
        }

        return gerarResumoImportacao("exemplares", totalSalvos, logErros);
    }

    // ==========================================================
    // 🔹 MÉTODOS AUXILIARES
    // ==========================================================
    private void validarArquivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio ou nulo");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
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
            if (v == null || v.isBlank()) return true;
        }
        return false;
    }

    private String normalizeNumber(String value) {
        if (value == null) return null;
        return value.replaceAll("\\D", "");
    }

    private boolean isEmailValido(String email) {
        if (email == null || email.isBlank()) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean isAlunoValidoParaSalvar(AlunoModel aluno) {
        return aluno.getMatricula() != null && !aluno.getMatricula().isBlank() &&
               aluno.getNomeCompleto() != null && !aluno.getNomeCompleto().isBlank() &&
               aluno.getCurso() != null;
    }

    private String extrairRootCause(Exception e) {
        Throwable rootCause = e;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause.getMessage();
    }

    private String gerarResumoImportacao(String tipo, int totalSalvos, List<ErroImportacao> logErros) {
        String resumo = String.format(
            "✅ Importação de %s concluída. Salvos: %d | Erros: %d",
            tipo, totalSalvos, logErros.size()
        );
        
        if (!logErros.isEmpty()) {
            String detalhes = logErros.stream()
                .map(ErroImportacao::toString)
                .limit(10) // Limita a 10 erros no resumo
                .collect(Collectors.joining("; "));
            resumo += " | Primeiros erros: " + detalhes;
            
            if (logErros.size() > 10) {
                resumo += " ... (+" + (logErros.size() - 10) + " mais)";
            }
        }
        
        log.info(resumo);
        return resumo;
    }

    // ==========================================================
    // 🔹 CLASSE DE ERROS
    // ==========================================================
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