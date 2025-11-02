package br.com.lumilivre.api.service; // Criar um Novo Serviço para a Migração

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class ImportacaoLegadoService {

    private static final Logger log = LoggerFactory.getLogger(ImportacaoLegadoService.class);

    private final LivroRepository livroRepository;
    private final ExemplarRepository exemplarRepository;
    private final CddRepository cddRepository;
    private final GeneroRepository generoRepository;

    private static final int BATCH_SIZE = 50;

    public ImportacaoLegadoService(
            LivroRepository livroRepository,
            ExemplarRepository exemplarRepository,
            CddRepository cddRepository,
            GeneroRepository generoRepository) {
        this.livroRepository = livroRepository;
        this.exemplarRepository = exemplarRepository;
        this.cddRepository = cddRepository;
        this.generoRepository = generoRepository;
    }

    // ==========================================================
    // 📚 IMPORTAÇÃO DE LIVROS (LÓGICA LEGADA)
    // ==========================================================
    @Transactional
    public String importarLivros(MultipartFile file) throws Exception {
        List<LivroModel> livrosParaSalvar = new ArrayList<>();
        List<ErroImportacao> logErros = new ArrayList<>();
        Set<String> isbnsNoExcel = new HashSet<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Pula cabeçalho

                int linhaNum = row.getRowNum() + 1;
                try {
                    String isbn = ExcelUtils.getString(row.getCell(1));
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
        // Coluna 0 (id da planilha) é ignorada.
        livro.setIsbn(ExcelUtils.getString(row.getCell(1)));

        String cddCodigo = ExcelUtils.getString(row.getCell(2));
        if (cddCodigo == null || cddCodigo.isBlank()) {
            throw new IllegalArgumentException("cdd_codigo (coluna 3) é obrigatório.");
        }
        CddModel cdd = cddRepository.findById(cddCodigo)
                .orElseThrow(() -> new IllegalArgumentException("CDD '" + cddCodigo + "' não encontrado no banco."));
        livro.setCdd(cdd);

        // Coluna 3 (CDD descrição) é ignorada.
        livro.setNome(ExcelUtils.getString(row.getCell(4)));
        livro.setAutor(ExcelUtils.getString(row.getCell(5)));
        livro.setEditora(ExcelUtils.getString(row.getCell(6)));
        livro.setData_lancamento(ExcelUtils.getLocalDate(row.getCell(7)));
        livro.setEdicao(ExcelUtils.getString(row.getCell(8)));
        // Coluna 9 (quantidade) é ignorada, será calculada pelos exemplares.
        livro.setNumero_paginas(ExcelUtils.getInteger(row.getCell(10)));
        livro.setClassificacao_etaria(ExcelUtils.getEnum(row.getCell(11), ClassificacaoEtaria.class, ClassificacaoEtaria.LIVRE));
        livro.setVolume(ExcelUtils.getInteger(row.getCell(12)));
        livro.setSinopse(ExcelUtils.getString(row.getCell(13)));
        livro.setTipo_capa(ExcelUtils.getEnum(row.getCell(14), TipoCapa.class, TipoCapa.BROCHURA));
        livro.setImagem(ExcelUtils.getString(row.getCell(15)));

        // Lógica automática de gênero baseada no CDD
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
            } catch (Exception e) {
                logErros.add(new ErroImportacao(-1, "Erro no lote " + (i / BATCH_SIZE + 1) + ": " + extrairRootCause(e)));
            }
        }
        return gerarResumoImportacao("livros legados", totalSalvos, logErros);
    }

    // ==========================================================
    // 📦 IMPORTAÇÃO DE EXEMPLARES (LÓGICA LEGADA)
    // ==========================================================
    @Transactional
    public String importarExemplares(MultipartFile file) throws Exception {
        List<ExemplarModel> exemplaresParaSalvar = new ArrayList<>();
        List<ErroImportacao> logErros = new ArrayList<>();
        Set<String> tombosNoExcel = new HashSet<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                int linhaNum = row.getRowNum() + 1;
                try {
                    String tombo = ExcelUtils.getString(row.getCell(1));
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
                .orElseThrow(() -> new IllegalArgumentException("Livro com ID '" + livroId + "' não encontrado no banco."));

        ExemplarModel exemplar = new ExemplarModel();
        exemplar.setLivro(livro);
        exemplar.setTombo(ExcelUtils.getString(row.getCell(1)));
        exemplar.setStatus_livro(ExcelUtils.getEnum(row.getCell(2), StatusLivro.class, StatusLivro.DISPONIVEL));
        exemplar.setLocalizacao_fisica(ExcelUtils.getString(row.getCell(3)));
        
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
            } catch (Exception e) {
                logErros.add(new ErroImportacao(-1, "Erro no lote " + (i / BATCH_SIZE + 1) + ": " + extrairRootCause(e)));
            }
        }

        // Após salvar todos os lotes, atualiza a contagem
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

        return gerarResumoImportacao("exemplares legados", totalSalvos, logErros);
    }

    // ==========================================================
    // 🔹 MÉTODOS AUXILIARES E CLASSE DE ERRO
    // ==========================================================
    private String extrairRootCause(Exception e) {
        Throwable rootCause = e;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause.getMessage();
    }

    private String gerarResumoImportacao(String tipo, int totalSalvos, List<ErroImportacao> logErros) {
        String resumo = String.format("✅ Importação de %s concluída. Salvos: %d | Erros: %d", tipo, totalSalvos, logErros.size());
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