package br.com.lumilivre.api.service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import br.com.lumilivre.api.enums.AgeRating;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.CoverType;
import br.com.lumilivre.api.enums.Role;
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
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);
    private static final int BATCH_SIZE = 50;

    private final StudentRepository alunoRepository;
    private final AppUserRepository usuarioRepository;
    private final CourseRepository courseRepository;
    private final StudyShiftRepository studyShiftRepository;
    private final AcademicModuleRepository academicModuleRepository;
    private final BookRepository livroRepository;
    private final BookCopyRepository exemplarRepository;
    private final DeweyClassificationRepository deweyClassificationRepository;
    private final PasswordEncoder passwordEncoder;

    public ImportService(
            StudentRepository alunoRepository,
            AppUserRepository usuarioRepository,
            CourseRepository courseRepository,
            StudyShiftRepository studyShiftRepository,
            AcademicModuleRepository academicModuleRepository,
            BookRepository livroRepository,
            BookCopyRepository exemplarRepository,
            DeweyClassificationRepository deweyClassificationRepository,
            PasswordEncoder passwordEncoder) {
        this.alunoRepository = alunoRepository;
        this.usuarioRepository = usuarioRepository;
        this.courseRepository = courseRepository;
        this.studyShiftRepository = studyShiftRepository;
        this.academicModuleRepository = academicModuleRepository;
        this.livroRepository = livroRepository;
        this.exemplarRepository = exemplarRepository;
        this.deweyClassificationRepository = deweyClassificationRepository;
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
        List<Student> alunosParaSalvar = new ArrayList<>();
        List<ErroImportacao> logErros = new ArrayList<>();
        Set<String> matriculasNoExcel = new HashSet<>();

        Set<String> matriculasExistentes = alunoRepository.findAllMatriculas();
        Set<String> cpfsExistentes = alunoRepository.findAllCpfs();

        Map<Integer, Course> coursesMap = courseRepository.findAll().stream()
                .collect(Collectors.toMap(Course::getId, c -> c));
        Map<Integer, StudyShift> studyShiftsMap = studyShiftRepository.findAll().stream()
                .collect(Collectors.toMap(StudyShift::getId, t -> t));
        Map<Integer, AcademicModule> academicModulesMap = academicModuleRepository.findAll().stream()
                .collect(Collectors.toMap(AcademicModule::getId, m -> m));

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

                    Student aluno = criarAlunoFromRow(row, headerMap, coursesMap, studyShiftsMap, academicModulesMap);

                    if (aluno.getCpf() != null && cpfsExistentes.contains(aluno.getCpf())) {
                        logErros.add(new ErroImportacao(linhaNum, "CPF já existe no sistema: " + aluno.getCpf()));
                        continue;
                    }
                    if (usuarioRepository.existsByEmail(aluno.getEmail())) {
                        logErros.add(new ErroImportacao(linhaNum, "Email já vinculado a um usuário: " + aluno.getEmail()));
                        continue;
                    }

                    AppUser usuario = new AppUser();
                    usuario.setEmail(aluno.getEmail());
                    usuario.setPasswordHash(passwordEncoder.encode(aluno.getRegistrationNumber()));
                    usuario.setRole(Role.STUDENT);
                    usuario.setStudent(aluno);
                    aluno.setAppUser(usuario);

                    alunosParaSalvar.add(aluno);

                } catch (Exception e) {
                    logErros.add(new ErroImportacao(linhaNum, "Erro: " + e.getMessage()));
                }
            }
        }

        return salvarEmLotes(alunosParaSalvar, alunoRepository, "alunos", logErros);
    }

    private Student criarAlunoFromRow(Row row, Map<String, Integer> headerMap,
            Map<Integer, Course> courses,
            Map<Integer, StudyShift> studyShifts,
            Map<Integer, AcademicModule> academicModules) {
        Student aluno = new Student();
        aluno.setRegistrationNumber(ExcelUtils.getString(row.getCell(headerMap.get("matricula"))));
        aluno.setFullName(ExcelUtils.getString(row.getCell(headerMap.get("nome_completo"))));
        aluno.setCpf(normalizeNumber(ExcelUtils.getString(row.getCell(headerMap.get("cpf")))));
        aluno.setPhoneNumber(normalizeNumber(ExcelUtils.getString(row.getCell(headerMap.get("celular")))));
        aluno.setEmail(ExcelUtils.getString(row.getCell(headerMap.get("email"))));
        aluno.setBirthDate(ExcelUtils.getLocalDate(row.getCell(headerMap.get("data_nascimento"))));

        aluno.setPostalCode(normalizeNumber(ExcelUtils.getString(row.getCell(headerMap.get("cep")))));
        aluno.setStreet(ExcelUtils.getString(row.getCell(headerMap.get("logradouro"))));
        aluno.setDistrict(ExcelUtils.getString(row.getCell(headerMap.get("bairro"))));
        aluno.setCity(ExcelUtils.getString(row.getCell(headerMap.get("localidade"))));
        aluno.setStateCode(ExcelUtils.getString(row.getCell(headerMap.get("uf"))));
        aluno.setStreetNumber(ExcelUtils.getInteger(row.getCell(headerMap.get("numero_casa"))));
        aluno.setAddressComplement(ExcelUtils.getString(row.getCell(headerMap.get("complemento"))));

        Integer cursoId = ExcelUtils.getInteger(row.getCell(headerMap.get("curso_id")));
        Integer turnoId = ExcelUtils.getInteger(row.getCell(headerMap.get("turno_id")));
        Integer moduloId = ExcelUtils.getInteger(row.getCell(headerMap.get("modulo_id")));

        if (cursoId != null && courses.containsKey(cursoId))
            aluno.setCourse(courses.get(cursoId));
        else
            throw new IllegalArgumentException("ID do Curso inválido ou não encontrado: " + cursoId);

        if (turnoId != null && studyShifts.containsKey(turnoId))
            aluno.setStudyShift(studyShifts.get(turnoId));
        if (moduloId != null && academicModules.containsKey(moduloId))
            aluno.setAcademicModule(academicModules.get(moduloId));

        return aluno;
    }

    // ===================== IMPORTAÇÃO DE LIVROS =====================

    @Transactional
    protected String importarLivros(MultipartFile file) throws Exception {
        List<Book> livrosParaSalvar = new ArrayList<>();
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

                    Book livro = criarLivroFromRow(row, headerMap);
                    livrosParaSalvar.add(livro);

                } catch (Exception e) {
                    logErros.add(new ErroImportacao(linhaNum, "Erro: " + e.getMessage()));
                }
            }
        }
        return salvarEmLotes(livrosParaSalvar, livroRepository, "livros", logErros);
    }

    private Book criarLivroFromRow(Row row, Map<String, Integer> headerMap) {
        Book livro = new Book();
        livro.setIsbn(ExcelUtils.getString(row.getCell(headerMap.get("isbn"))));
        livro.setTitle(ExcelUtils.getString(row.getCell(headerMap.get("nome"))));
        livro.setAuthor(ExcelUtils.getString(row.getCell(headerMap.get("autor"))));
        livro.setPublisher(ExcelUtils.getString(row.getCell(headerMap.get("editora"))));
        livro.setPublicationDate(ExcelUtils.getLocalDate(row.getCell(headerMap.get("data_lancamento"))));
        livro.setPageCount(ExcelUtils.getInteger(row.getCell(headerMap.get("numero_paginas"))));
        livro.setEdition(ExcelUtils.getString(row.getCell(headerMap.get("edicao"))));
        livro.setVolume(ExcelUtils.getInteger(row.getCell(headerMap.get("volume"))));
        livro.setSynopsis(ExcelUtils.getString(row.getCell(headerMap.get("sinopse"))));
        livro.setCoverUrl(ExcelUtils.getString(row.getCell(headerMap.get("imagem"))));

        livro.setAgeRating(ExcelUtils.getEnum(row.getCell(headerMap.get("classificacao_etaria")),
                AgeRating.class, AgeRating.GENERAL));
        livro.setCoverType(
                ExcelUtils.getEnum(row.getCell(headerMap.get("tipo_capa")), CoverType.class, CoverType.PAPERBACK));

        String cddCodigo = ExcelUtils.getString(row.getCell(headerMap.get("cdd_codigo")));
        if (cddCodigo == null || cddCodigo.isBlank())
            throw new IllegalArgumentException("CDD é obrigatório");

        DeweyClassification cdd = deweyClassificationRepository.findById(cddCodigo)
                .orElseThrow(() -> new IllegalArgumentException("CDD não encontrado: " + cddCodigo));
        livro.setDeweyClassification(cdd);

        livro.setGenres(new HashSet<>());

        return livro;
    }

    // ===================== IMPORTAÇÃO DE EXEMPLARES =====================

    @Transactional
    protected String importarExemplares(MultipartFile file) throws Exception {
        List<BookCopy> exemplaresParaSalvar = new ArrayList<>();
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
                    if (exemplarRepository.existsByCopyCode(tombo)) {
                        logErros.add(new ErroImportacao(linhaNum, "Tombo já existe no sistema: " + tombo));
                        continue;
                    }

                    BookCopy exemplar = criarExemplarFromRow(row, headerMap);
                    exemplaresParaSalvar.add(exemplar);

                } catch (Exception e) {
                    logErros.add(new ErroImportacao(linhaNum, "Erro: " + e.getMessage()));
                }
            }
        }

        return salvarEmLotes(exemplaresParaSalvar, exemplarRepository, "exemplares", logErros);
    }

    private BookCopy criarExemplarFromRow(Row row, Map<String, Integer> headerMap) {
        String livroIdStr = ExcelUtils.getString(row.getCell(headerMap.get("livro_id")));
        if (livroIdStr == null || livroIdStr.isBlank())
            throw new IllegalArgumentException("ID do Livro é obrigatório");

        UUID livroId;
        try {
            livroId = UUID.fromString(livroIdStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("ID do Livro inválido (deve ser UUID): " + livroIdStr);
        }

        Book livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new IllegalArgumentException("Livro não encontrado ID: " + livroId));

        BookCopy exemplar = new BookCopy();
        exemplar.setCopyCode(ExcelUtils.getString(row.getCell(headerMap.get("tombo"))));
        exemplar.setShelfLocation(ExcelUtils.getString(row.getCell(headerMap.get("localizacao_fisica"))));
        exemplar.setStatus(ExcelUtils.getEnum(row.getCell(headerMap.get("status_livro")), BookCopyStatus.class,
                BookCopyStatus.AVAILABLE));
        exemplar.setBook(livro);

        return exemplar;
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
                erros.add(new ErroImportacao(-1, "Erro ao salvar lote " + (i / BATCH_SIZE + 1) + ": " + e.getMessage()));
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
