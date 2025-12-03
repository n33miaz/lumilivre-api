package br.com.lumilivre.api.service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import br.com.lumilivre.api.enums.ClassificacaoEtaria;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.enums.StatusLivro;
import br.com.lumilivre.api.enums.TipoCapa;
import br.com.lumilivre.api.model.*;
import br.com.lumilivre.api.repository.*;
import br.com.lumilivre.api.utils.ExcelUtils;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportacaoService {

    private static final Logger log = LoggerFactory.getLogger(ImportacaoService.class);
    private static final int BATCH_SIZE = 50;

    private final AlunoRepository alunoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final TurnoRepository turnoRepository;
    private final ModuloRepository moduloRepository;
    private final LivroRepository livroRepository;
    private final ExemplarRepository exemplarRepository;
    private final CddRepository cddRepository;
    private final PasswordEncoder passwordEncoder;

    public ImportacaoService(
            AlunoRepository alunoRepository,
            UsuarioRepository usuarioRepository,
            CursoRepository cursoRepository,
            TurnoRepository turnoRepository,
            ModuloRepository moduloRepository,
            LivroRepository livroRepository,
            ExemplarRepository exemplarRepository,
            CddRepository cddRepository,
            PasswordEncoder passwordEncoder) {
        this.alunoRepository = alunoRepository;
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
        this.turnoRepository = turnoRepository;
        this.moduloRepository = moduloRepository;
        this.livroRepository = livroRepository;
        this.exemplarRepository = exemplarRepository;
        this.cddRepository = cddRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String importar(String tipo, MultipartFile file) throws Exception {
        log.info("Iniciando importação unificada do tipo: {}", tipo);
        validarArquivo(file);

        try {
            return switch (tipo.toLowerCase()) {
                case "aluno" -> importarAlunos(file);
                case "livro" -> importarLivros(file);
                case "exemplar" -> importarExemplares(file);
                default -> throw new IllegalArgumentException("Tipo de importação inválido: " + tipo);
            };
        } catch (Exception e) {
            log.error("Erro crítico durante importação do tipo {}: {}", tipo, e.getMessage(), e);
            throw new Exception("Falha na importação: " + e.getMessage(), e);
        }
    }

    // ==================== IMPORTAÇÃO DE ALUNOS =====================

    @Transactional
    protected String importarAlunos(MultipartFile file) throws Exception {
        List<AlunoModel> alunosParaSalvar = new ArrayList<>();
        List<ErroImportacao> logErros = new ArrayList<>();
        Set<String> matriculasNoExcel = new HashSet<>();

        Set<String> matriculasExistentes = alunoRepository.findAllMatriculas();
        Set<String> cpfsExistentes = alunoRepository.findAllCpfs();

        Map<Integer, CursoModel> cursosMap = cursoRepository.findAll().stream()
                .collect(Collectors.toMap(CursoModel::getId, c -> c));
        Map<Integer, TurnoModel> turnosMap = turnoRepository.findAll().stream()
                .collect(Collectors.toMap(TurnoModel::getId, t -> t));
        Map<Integer, ModuloModel> modulosMap = moduloRepository.findAll().stream()
                .collect(Collectors.toMap(ModuloModel::getId, m -> m));

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headerMap = mapearCabecalhos(sheet.getRow(0));

            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue;
                int linhaNum = row.getRowNum() + 1;

                try {
                    String matricula = ExcelUtils.getString(row.getCell(headerMap.get("matricula")));

                    if (matricula.isBlank()) {
                        logErros.add(new ErroImportacao(linhaNum, "Matrícula vazia."));
                        continue;
                    }
                    if (!matriculasNoExcel.add(matricula)) {
                        logErros.add(new ErroImportacao(linhaNum, "Matrícula duplicada na planilha: " + matricula));
                        continue;
                    }
                    if (matriculasExistentes.contains(matricula)) {
                        logErros.add(new ErroImportacao(linhaNum, "Aluno já cadastrado no sistema: " + matricula));
                        continue;
                    }

                    AlunoModel aluno = criarAlunoFromRow(row, headerMap, cursosMap, turnosMap, modulosMap);

                    if (aluno.getCpf() != null && cpfsExistentes.contains(aluno.getCpf())) {
                        logErros.add(new ErroImportacao(linhaNum, "CPF já existe no sistema: " + aluno.getCpf()));
                        continue;
                    }
                    if (usuarioRepository.existsByEmail(aluno.getEmail())) {
                        logErros.add(
                                new ErroImportacao(linhaNum, "Email já vinculado a um usuário: " + aluno.getEmail()));
                        continue;
                    }

                    UsuarioModel usuario = new UsuarioModel();
                    usuario.setEmail(aluno.getEmail());
                    usuario.setSenha(passwordEncoder.encode(aluno.getMatricula())); // Senha padrão é a matrícula
                    usuario.setRole(Role.ALUNO);
                    usuario.setAluno(aluno);
                    aluno.setUsuario(usuario);

                    alunosParaSalvar.add(aluno);

                } catch (Exception e) {
                    logErros.add(new ErroImportacao(linhaNum, "Erro: " + e.getMessage()));
                }
            }
        }

        return salvarEmLotes(alunosParaSalvar, alunoRepository, "alunos", logErros);
    }

    private AlunoModel criarAlunoFromRow(Row row, Map<String, Integer> headerMap,
            Map<Integer, CursoModel> cursos,
            Map<Integer, TurnoModel> turnos,
            Map<Integer, ModuloModel> modulos) {
        AlunoModel aluno = new AlunoModel();
        aluno.setMatricula(ExcelUtils.getString(row.getCell(headerMap.get("matricula"))));
        aluno.setNomeCompleto(ExcelUtils.getString(row.getCell(headerMap.get("nome_completo"))));
        aluno.setCpf(normalizeNumber(ExcelUtils.getString(row.getCell(headerMap.get("cpf")))));
        aluno.setCelular(normalizeNumber(ExcelUtils.getString(row.getCell(headerMap.get("celular")))));
        aluno.setEmail(ExcelUtils.getString(row.getCell(headerMap.get("email"))));
        aluno.setDataNascimento(ExcelUtils.getLocalDate(row.getCell(headerMap.get("data_nascimento"))));

        aluno.setCep(normalizeNumber(ExcelUtils.getString(row.getCell(headerMap.get("cep")))));
        aluno.setLogradouro(ExcelUtils.getString(row.getCell(headerMap.get("logradouro"))));
        aluno.setBairro(ExcelUtils.getString(row.getCell(headerMap.get("bairro"))));
        aluno.setLocalidade(ExcelUtils.getString(row.getCell(headerMap.get("localidade"))));
        aluno.setUf(ExcelUtils.getString(row.getCell(headerMap.get("uf"))));
        aluno.setNumero_casa(ExcelUtils.getInteger(row.getCell(headerMap.get("numero_casa"))));
        aluno.setComplemento(ExcelUtils.getString(row.getCell(headerMap.get("complemento"))));

        Integer cursoId = ExcelUtils.getInteger(row.getCell(headerMap.get("curso_id")));
        Integer turnoId = ExcelUtils.getInteger(row.getCell(headerMap.get("turno_id")));
        Integer moduloId = ExcelUtils.getInteger(row.getCell(headerMap.get("modulo_id")));

        if (cursoId != null && cursos.containsKey(cursoId))
            aluno.setCurso(cursos.get(cursoId));
        else
            throw new IllegalArgumentException("ID do Curso inválido ou não encontrado: " + cursoId);

        if (turnoId != null && turnos.containsKey(turnoId))
            aluno.setTurno(turnos.get(turnoId));
        if (moduloId != null && modulos.containsKey(moduloId))
            aluno.setModulo(modulos.get(moduloId));

        return aluno;
    }

    // ===================== IMPORTAÇÃO DE LIVROS =====================

    @Transactional
    protected String importarLivros(MultipartFile file) throws Exception {
        List<LivroModel> livrosParaSalvar = new ArrayList<>();
        List<ErroImportacao> logErros = new ArrayList<>();
        Set<String> isbnsNoExcel = new HashSet<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headerMap = mapearCabecalhos(sheet.getRow(0));

            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue;
                int linhaNum = row.getRowNum() + 1;

                try {
                    String isbn = ExcelUtils.getString(row.getCell(headerMap.get("isbn")));

                    if (isbn != null && !isbn.isBlank()) {
                        if (!isbnsNoExcel.add(isbn)) {
                            logErros.add(new ErroImportacao(linhaNum, "ISBN duplicado na planilha: " + isbn));
                            continue;
                        }
                        if (livroRepository.existsByIsbn(isbn)) {
                            logErros.add(new ErroImportacao(linhaNum, "ISBN já cadastrado no sistema: " + isbn));
                            continue;
                        }
                    }

                    LivroModel livro = criarLivroFromRow(row, headerMap);
                    livrosParaSalvar.add(livro);

                } catch (Exception e) {
                    logErros.add(new ErroImportacao(linhaNum, "Erro: " + e.getMessage()));
                }
            }
        }
        return salvarEmLotes(livrosParaSalvar, livroRepository, "livros", logErros);
    }

    private LivroModel criarLivroFromRow(Row row, Map<String, Integer> headerMap) {
        LivroModel livro = new LivroModel();
        livro.setIsbn(ExcelUtils.getString(row.getCell(headerMap.get("isbn"))));
        livro.setNome(ExcelUtils.getString(row.getCell(headerMap.get("nome"))));
        livro.setAutor(ExcelUtils.getString(row.getCell(headerMap.get("autor"))));
        livro.setEditora(ExcelUtils.getString(row.getCell(headerMap.get("editora"))));
        livro.setData_lancamento(ExcelUtils.getLocalDate(row.getCell(headerMap.get("data_lancamento"))));
        livro.setNumero_paginas(ExcelUtils.getInteger(row.getCell(headerMap.get("numero_paginas"))));
        livro.setEdicao(ExcelUtils.getString(row.getCell(headerMap.get("edicao"))));
        livro.setVolume(ExcelUtils.getInteger(row.getCell(headerMap.get("volume"))));
        livro.setSinopse(ExcelUtils.getString(row.getCell(headerMap.get("sinopse"))));
        livro.setImagem(ExcelUtils.getString(row.getCell(headerMap.get("imagem"))));

        livro.setClassificacao_etaria(ExcelUtils.getEnum(row.getCell(headerMap.get("classificacao_etaria")),
                ClassificacaoEtaria.class, ClassificacaoEtaria.LIVRE));
        livro.setTipo_capa(
                ExcelUtils.getEnum(row.getCell(headerMap.get("tipo_capa")), TipoCapa.class, TipoCapa.BROCHURA));

        String cddCodigo = ExcelUtils.getString(row.getCell(headerMap.get("cdd_codigo")));
        if (cddCodigo == null || cddCodigo.isBlank())
            throw new IllegalArgumentException("CDD é obrigatório");

        CddModel cdd = cddRepository.findById(cddCodigo)
                .orElseThrow(() -> new IllegalArgumentException("CDD não encontrado: " + cddCodigo));
        livro.setCdd(cdd);

        livro.setGeneros(new HashSet<>());

        return livro;
    }

    // ===================== IMPORTAÇÃO DE EXEMPLARES =====================

    @Transactional
    protected String importarExemplares(MultipartFile file) throws Exception {
        List<ExemplarModel> exemplaresParaSalvar = new ArrayList<>();
        List<ErroImportacao> logErros = new ArrayList<>();
        Set<String> tombosNoExcel = new HashSet<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headerMap = mapearCabecalhos(sheet.getRow(0));

            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue;
                int linhaNum = row.getRowNum() + 1;

                try {
                    String tombo = ExcelUtils.getString(row.getCell(headerMap.get("tombo")));
                    if (tombo.isBlank()) {
                        logErros.add(new ErroImportacao(linhaNum, "Tombo obrigatório."));
                        continue;
                    }
                    if (!tombosNoExcel.add(tombo)) {
                        logErros.add(new ErroImportacao(linhaNum, "Tombo duplicado na planilha: " + tombo));
                        continue;
                    }
                    if (exemplarRepository.existsById(tombo)) {
                        logErros.add(new ErroImportacao(linhaNum, "Tombo já existe no sistema: " + tombo));
                        continue;
                    }

                    ExemplarModel exemplar = criarExemplarFromRow(row, headerMap);
                    exemplaresParaSalvar.add(exemplar);

                } catch (Exception e) {
                    logErros.add(new ErroImportacao(linhaNum, "Erro: " + e.getMessage()));
                }
            }
        }

        String resultado = salvarEmLotes(exemplaresParaSalvar, exemplarRepository, "exemplares", logErros);

        if (!exemplaresParaSalvar.isEmpty()) {
            atualizarContagemLivros(exemplaresParaSalvar);
        }

        return resultado;
    }

    private ExemplarModel criarExemplarFromRow(Row row, Map<String, Integer> headerMap) {
        Long livroId = ExcelUtils.getLong(row.getCell(headerMap.get("livro_id")));
        if (livroId == null)
            throw new IllegalArgumentException("ID do Livro é obrigatório");

        LivroModel livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new IllegalArgumentException("Livro não encontrado ID: " + livroId));

        ExemplarModel exemplar = new ExemplarModel();
        exemplar.setTombo(ExcelUtils.getString(row.getCell(headerMap.get("tombo"))));
        exemplar.setLocalizacao_fisica(ExcelUtils.getString(row.getCell(headerMap.get("localizacao_fisica"))));
        exemplar.setStatus_livro(ExcelUtils.getEnum(row.getCell(headerMap.get("status_livro")), StatusLivro.class,
                StatusLivro.DISPONIVEL));
        exemplar.setLivro(livro);

        return exemplar;
    }

    private void atualizarContagemLivros(List<ExemplarModel> exemplares) {
        Set<Long> livrosIds = exemplares.stream().map(e -> e.getLivro().getId()).collect(Collectors.toSet());
        for (Long id : livrosIds) {
            long count = exemplarRepository.countByLivroId(id);
            livroRepository.findById(id).ifPresent(l -> {
                l.setQuantidade((int) count);
                livroRepository.save(l);
            });
        }
    }

    // ===================== UTILITÁRIOS =====================

    private <T> String salvarEmLotes(List<T> lista, JpaRepository<T, ?> repository, String nomeEntidade,
            List<ErroImportacao> erros) {
        int salvos = 0;
        for (int i = 0; i < lista.size(); i += BATCH_SIZE) {
            int fim = Math.min(i + BATCH_SIZE, lista.size());
            List<T> lote = lista.subList(i, fim);
            try {
                repository.saveAll(lote);
                salvos += lote.size();
            } catch (Exception e) {
                erros.add(
                        new ErroImportacao(-1, "Erro ao salvar lote " + (i / BATCH_SIZE + 1) + ": " + e.getMessage()));
                log.error("Erro ao salvar lote de {}", nomeEntidade, e);
            }
        }
        return gerarResumo(nomeEntidade, salvos, erros);
    }

    private Map<String, Integer> mapearCabecalhos(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (Cell cell : headerRow) {
            if (cell.getCellType() == CellType.STRING) {
                map.put(cell.getStringCellValue().trim().toLowerCase().replace(" ", "_"), cell.getColumnIndex());
            }
        }
        return map;
    }

    private void validarArquivo(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("Arquivo vazio.");
        if (!Objects.equals(file.getContentType(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            throw new IllegalArgumentException("Formato inválido. Use .xlsx");
        }
    }

    private String normalizeNumber(String val) {
        return val == null ? null : val.replaceAll("\\D", "");
    }

    private String gerarResumo(String tipo, int salvos, List<ErroImportacao> erros) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Importação de %s concluída. Salvos: %d. Erros: %d.", tipo, salvos, erros.size()));
        if (!erros.isEmpty()) {
            sb.append(" Detalhes: ")
                    .append(erros.stream().limit(5).map(ErroImportacao::toString).collect(Collectors.joining("; ")));
            if (erros.size() > 5)
                sb.append("...");
        }
        return sb.toString();
    }

    private record ErroImportacao(int linha, String erro) {
        @Override
        public String toString() {
            return (linha > 0 ? "Linha " + linha + ": " : "") + erro;
        }
    }
}