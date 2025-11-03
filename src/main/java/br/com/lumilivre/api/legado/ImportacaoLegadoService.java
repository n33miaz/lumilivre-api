package br.com.lumilivre.api.legado;

import br.com.lumilivre.api.enums.ClassificacaoEtaria;
import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.enums.TipoCapa;
import br.com.lumilivre.api.model.CddModel;
import br.com.lumilivre.api.model.ExemplarModel;
import br.com.lumilivre.api.model.GeneroModel;
import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.repository.CddRepository;
import br.com.lumilivre.api.repository.ExemplarRepository;
import br.com.lumilivre.api.repository.GeneroRepository;
import br.com.lumilivre.api.repository.LivroRepository;
import br.com.lumilivre.api.utils.ExcelUtils;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImportacaoLegadoService {

    private static final Logger log = LoggerFactory.getLogger(ImportacaoLegadoService.class);

    private final LivroRepository livroRepository;
    private final ExemplarRepository exemplarRepository;
    private final CddRepository cddRepository;
    private final GeneroRepository generoRepository;

    private static final int BATCH_SIZE = 100;

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

    // =============== IMPORTAÇÃO DE LIVROS (LÓGICA LEGADA) ===============
    @Transactional
    public String importarLivros(MultipartFile file) throws Exception {
        List<LivroModel> livrosParaSalvar = new ArrayList<>();
        List<ErroImportacao> logErros = new ArrayList<>();
        Set<String> isbnsNoExcel = new HashSet<>();
        Set<Long> idsNoExcel = new HashSet<>();
        Map<String, CddModel> cddCache = new HashMap<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            
            Map<String, Integer> headerMap = mapearCabecalhos(sheet.getRow(0));
            validarCabecalhosObrigatorios(headerMap, List.of("id", "cdd_codigo", "nome", "autor", "editora", "data_lancamento", "numero_paginas"));

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                int linhaNum = row.getRowNum() + 1;
                try {
                    Long id = ExcelUtils.getLong(row.getCell(headerMap.get("id")));
                    if (id == null) {
                        logErros.add(new ErroImportacao(linhaNum, "Coluna 'id' é obrigatória e não pode ser nula."));
                        continue;
                    }
                    if (!idsNoExcel.add(id)) {
                        logErros.add(new ErroImportacao(linhaNum, "ID duplicado no Excel: " + id));
                        continue;
                    }
                    if (livroRepository.existsById(id)) {
                        logErros.add(new ErroImportacao(linhaNum, "Livro com este ID já existe no banco: " + id));
                        continue;
                    }

                    String isbn = ExcelUtils.getString(row.getCell(headerMap.get("isbn")));
                    if ("0".equals(isbn) || isbn.isBlank()) {
                        isbn = null;
                    }

                    if (isbn != null) {
                        if (!isbnsNoExcel.add(isbn)) {
                            logErros.add(new ErroImportacao(linhaNum, "ISBN duplicado no Excel: " + isbn));
                            continue;
                        }
                        if (livroRepository.findByIsbn(isbn).isPresent()) {
                            logErros.add(new ErroImportacao(linhaNum, "Livro com este ISBN já existe: " + isbn));
                            continue;
                        }
                    }

                    LivroModel livro = criarLivroFromRow(row, cddCache, headerMap);
                    livrosParaSalvar.add(livro);

                } catch (Exception e) {
                    logErros.add(new ErroImportacao(linhaNum, "Erro ao processar livro: " + e.getMessage()));
                    log.warn("Falha na linha {}: {}", linhaNum, e.getMessage());
                }
            }
        }

        return salvarLivrosEmLotes(livrosParaSalvar, logErros);
    }

    private LivroModel criarLivroFromRow(Row row, Map<String, CddModel> cddCache, Map<String, Integer> headerMap) {
        LivroModel livro = new LivroModel();

        livro.setId(ExcelUtils.getLong(row.getCell(headerMap.get("id"))));
        
        String isbn = ExcelUtils.getString(row.getCell(headerMap.get("isbn")));
        livro.setIsbn("0".equals(isbn) || isbn.isBlank() ? null : isbn);

        String cddCodigo = ExcelUtils.getString(row.getCell(headerMap.get("cdd_codigo")));
        if (cddCodigo == null || cddCodigo.isBlank()) {
            throw new IllegalArgumentException("Coluna 'cdd_codigo' é obrigatória.");
        }
        
        CddModel cdd = cddCache.computeIfAbsent(cddCodigo, key -> 
            cddRepository.findById(key)
                .orElseThrow(() -> new IllegalArgumentException("CDD '" + key + "' não encontrado no banco."))
        );
        livro.setCdd(cdd);

        livro.setNome(ExcelUtils.getString(row.getCell(headerMap.get("nome"))));
        livro.setAutor(ExcelUtils.getString(row.getCell(headerMap.get("autor"))));
        livro.setEditora(ExcelUtils.getString(row.getCell(headerMap.get("editora"))));
        livro.setData_lancamento(ExcelUtils.getLocalDate(row.getCell(headerMap.get("data_lancamento"))));
        livro.setEdicao(ExcelUtils.getString(row.getCell(headerMap.get("edicao"))));
        
        Integer numeroPaginas = ExcelUtils.getInteger(row.getCell(headerMap.get("numero_paginas")));
        if (numeroPaginas == null || numeroPaginas <= 0) {
            throw new IllegalArgumentException("Coluna 'numero_paginas' é obrigatória e deve ser um número maior que zero.");
        }
        livro.setNumero_paginas(numeroPaginas);

        livro.setClassificacao_etaria(ExcelUtils.getEnum(row.getCell(headerMap.get("classificacao_etaria")), ClassificacaoEtaria.class, ClassificacaoEtaria.LIVRE));
        livro.setVolume(ExcelUtils.getInteger(row.getCell(headerMap.get("volume"))));
        livro.setSinopse(ExcelUtils.getString(row.getCell(headerMap.get("sinopse"))));
        livro.setTipo_capa(ExcelUtils.getEnum(row.getCell(headerMap.get("tipo_capa")), TipoCapa.class, TipoCapa.BROCHURA));
        livro.setImagem(ExcelUtils.getString(row.getCell(headerMap.get("imagem"))));

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
                livroRepository.saveAllAndFlush(subLista);
                totalSalvos += subLista.size();
            } catch (Exception e) {
                String erroMsg = "Erro no lote " + (i / BATCH_SIZE + 1) + ": " + extrairRootCause(e);
                logErros.add(new ErroImportacao(-1, erroMsg));
                log.error(erroMsg, e);
            }
        }
        return gerarResumoImportacao("livros legados", totalSalvos, logErros);
    }

    // =============== IMPORTAÇÃO DE EXEMPLARES (LÓGICA LEGADA) ===============
    @Transactional
    public String importarExemplares(MultipartFile file) throws Exception {
        List<ExemplarModel> exemplaresParaSalvar = new ArrayList<>();
        List<ErroImportacao> logErros = new ArrayList<>();
        Set<String> tombosNoExcel = new HashSet<>();
        Map<Long, LivroModel> livroCache = new HashMap<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headerMap = mapearCabecalhos(sheet.getRow(0));
            validarCabecalhosObrigatorios(headerMap, List.of("livro_id", "tombo", "status_livro", "localizacao_fisica"));

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                int linhaNum = row.getRowNum() + 1;
                try {
                    String tombo = ExcelUtils.getString(row.getCell(headerMap.get("tombo")));
                    if (tombo == null || tombo.isBlank()) {
                        logErros.add(new ErroImportacao(linhaNum, "Coluna 'tombo' é obrigatória."));
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
                    ExemplarModel exemplar = criarExemplarFromRow(row, livroCache, headerMap);
                    exemplaresParaSalvar.add(exemplar);
                } catch (Exception e) {
                    logErros.add(new ErroImportacao(linhaNum, "Erro ao processar exemplar: " + e.getMessage()));
                    log.warn("Falha na linha {}: {}", linhaNum, e.getMessage());
                }
            }
        }
        return salvarExemplaresEmLotes(exemplaresParaSalvar, logErros);
    }

    private ExemplarModel criarExemplarFromRow(Row row, Map<Long, LivroModel> livroCache, Map<String, Integer> headerMap) {
        Long livroId = ExcelUtils.getLong(row.getCell(headerMap.get("livro_id")));
        if (livroId == null) {
            throw new IllegalArgumentException("Coluna 'livro_id' é obrigatória.");
        }
        LivroModel livro = livroCache.computeIfAbsent(livroId, key -> livroRepository.findById(key)
                .orElseThrow(() -> new IllegalArgumentException("Livro com ID '" + key + "' não encontrado no banco.")));
        
        ExemplarModel exemplar = new ExemplarModel();
        exemplar.setLivro(livro);
        exemplar.setTombo(ExcelUtils.getString(row.getCell(headerMap.get("tombo"))));
        exemplar.setStatus_livro(ExcelUtils.getEnum(row.getCell(headerMap.get("status_livro")), StatusLivro.class, StatusLivro.DISPONIVEL));
        exemplar.setLocalizacao_fisica(ExcelUtils.getString(row.getCell(headerMap.get("localizacao_fisica"))));
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
                String erroMsg = "Erro no lote " + (i / BATCH_SIZE + 1) + ": " + extrairRootCause(e);
                logErros.add(new ErroImportacao(-1, erroMsg));
                log.error(erroMsg, e);
            }
        }
        if (totalSalvos > 0) {
            atualizarContagemDeExemplares(exemplares);
        }
        return gerarResumoImportacao("exemplares legados", totalSalvos, logErros);
    }

    private void atualizarContagemDeExemplares(List<ExemplarModel> exemplaresProcessados) {
        log.info("Atualizando contagem de exemplares nos livros...");
        Set<Long> livroIdsAfetados = exemplaresProcessados.stream()
                .map(e -> e.getLivro().getId())
                .collect(Collectors.toSet());
        
        List<LivroModel> livrosParaAtualizar = new ArrayList<>();
        for (Long livroId : livroIdsAfetados) {
            livroRepository.findById(livroId).ifPresent(livro -> {
                long novaQuantidade = exemplarRepository.countByLivroId(livro.getId());
                livro.setQuantidade((int) novaQuantidade);
                livrosParaAtualizar.add(livro);
            });
        }
        if (!livrosParaAtualizar.isEmpty()) {
            livroRepository.saveAll(livrosParaAtualizar);
            log.info("Contagem de exemplares atualizada para {} livros.", livrosParaAtualizar.size());
        }
    }

    // =============== MÉTODOS AUXILIARES E CLASSE DE ERRO ===============
    
    private Map<String, Integer> mapearCabecalhos(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        if (headerRow == null) {
            throw new IllegalArgumentException("A planilha esta vazia ou nao possui uma linha de cabecalho.");
        }
        for (Cell cell : headerRow) {
            if(cell != null && cell.getCellType() == CellType.STRING) {
                String headerText = cell.getStringCellValue().trim().toLowerCase();
                map.put(headerText, cell.getColumnIndex());
            }
        }
        return map;
    }

    private void validarCabecalhosObrigatorios(Map<String, Integer> headerMap, List<String> cabecalhosObrigatorios) {
        for (String header : cabecalhosObrigatorios) {
            if (!headerMap.containsKey(header)) {
                throw new IllegalArgumentException("Coluna obrigatória não encontrada na planilha: '" + header + "'");
            }
        }
    }

    private String extrairRootCause(Exception e) {
        Throwable rootCause = e;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause.getMessage();
    }

    private String gerarResumoImportacao(String tipo, int totalSalvos, List<ErroImportacao> logErros) {
        String resumo = String.format("Importacao de %s concluida. Salvos: %d | Erros: %d", tipo, totalSalvos, logErros.size());
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