package br.com.lumilivre.api.service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import br.com.lumilivre.api.enums.AgeRating;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.CoverType;
import br.com.lumilivre.api.enums.LibraryType;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.*;
import br.com.lumilivre.api.repository.*;
import br.com.lumilivre.api.config.MessageResolver;
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

    private final ReaderRepository leitorRepository;
    private final AppUserRepository usuarioRepository;
    private final CourseRepository courseRepository;
    private final StudyShiftRepository studyShiftRepository;
    private final AcademicModuleRepository academicModuleRepository;
    private final BookRepository livroRepository;
    private final BookCopyRepository exemplarRepository;
    private final DeweyClassificationRepository deweyClassificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessageResolver messages;
    private final SettingsService settingsService;

    public ImportService(
            ReaderRepository leitorRepository,
            AppUserRepository usuarioRepository,
            CourseRepository courseRepository,
            StudyShiftRepository studyShiftRepository,
            AcademicModuleRepository academicModuleRepository,
            BookRepository livroRepository,
            BookCopyRepository exemplarRepository,
            DeweyClassificationRepository deweyClassificationRepository,
            PasswordEncoder passwordEncoder,
            MessageResolver messages,
            SettingsService settingsService) {
        this.leitorRepository = leitorRepository;
        this.usuarioRepository = usuarioRepository;
        this.courseRepository = courseRepository;
        this.studyShiftRepository = studyShiftRepository;
        this.academicModuleRepository = academicModuleRepository;
        this.livroRepository = livroRepository;
        this.exemplarRepository = exemplarRepository;
        this.deweyClassificationRepository = deweyClassificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.messages = messages;
        this.settingsService = settingsService;
    }

    public String importar(String tipo, MultipartFile file) throws Exception {
        return importar(tipo, file, Locale.forLanguageTag("pt-BR"));
    }

    public String importar(String tipo, MultipartFile file, Locale locale) throws Exception {
        log.info("Iniciando importação unificada do tipo: {}", tipo);
        validarArquivo(file, locale);

        try {
            return switch (tipo.toLowerCase()) {
                case "leitor" -> importarLeitores(file, locale);
                case "livro" -> importarLivros(file, locale);
                case "exemplar" -> importarExemplares(file, locale);
                default -> throw new IllegalArgumentException(
                        messages.resolve("import.error.type.invalid", locale, tipo));
            };
        } catch (Exception e) {
            log.error("Erro crítico durante importação do tipo {}: {}", tipo, e.getMessage(), e);
            throw new Exception(messages.resolve("import.error.failure", locale, e.getMessage()), e);
        }
    }

    // ==================== IMPORTAÇÃO DE LEITORES ===================

    @Transactional
    protected String importarLeitores(MultipartFile file, Locale locale) throws Exception {
        List<Reader> leitoresParaSalvar = new ArrayList<>();
        List<ErroImportacao> logErros = new ArrayList<>();
        Set<String> matriculasNoExcel = new HashSet<>();

        Set<String> matriculasExistentes = leitorRepository.findAllMatriculas();
        Set<String> cpfsExistentes = leitorRepository.findAllCpfs();

        Map<Integer, Course> coursesMap = courseRepository.findAll().stream()
                .collect(Collectors.toMap(Course::getId, c -> c));
        Map<Integer, StudyShift> studyShiftsMap = studyShiftRepository.findAll().stream()
                .collect(Collectors.toMap(StudyShift::getId, t -> t));
        Map<Integer, AcademicModule> academicModulesMap = academicModuleRepository.findAll().stream()
                .collect(Collectors.toMap(AcademicModule::getId, m -> m));
        LibraryType libraryType = settingsService.getLibraryType();

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
                        logErros.add(new ErroImportacao(linhaNum, "import.error.registration.empty"));
                        continue;
                    }
                    if (!matriculasNoExcel.add(matricula)) {
                        logErros.add(new ErroImportacao(linhaNum,
                                "import.error.registration.duplicate-in-sheet", matricula));
                        continue;
                    }
                    if (matriculasExistentes.contains(matricula)) {
                        logErros.add(new ErroImportacao(linhaNum,
                                "import.error.registration.already-exists", matricula));
                        continue;
                    }

                    Reader leitor = criarLeitorFromRow(row, headerMap, coursesMap, studyShiftsMap,
                            academicModulesMap, libraryType, locale);

                    if (leitor.getCpf() != null && cpfsExistentes.contains(leitor.getCpf())) {
                        logErros.add(new ErroImportacao(linhaNum,
                                "import.error.cpf.already-exists", leitor.getCpf()));
                        continue;
                    }
                    if (usuarioRepository.existsByEmail(leitor.getEmail())) {
                        logErros.add(new ErroImportacao(linhaNum,
                                "import.error.email.already-linked", leitor.getEmail()));
                        continue;
                    }

                    AppUser usuario = new AppUser();
                    usuario.setEmail(leitor.getEmail());
                    usuario.setPasswordHash(passwordEncoder.encode(leitor.getRegistrationNumber()));
                    usuario.setRole(Role.READER);
                    usuario.setReader(leitor);
                    leitor.setAppUser(usuario);

                    leitoresParaSalvar.add(leitor);

                } catch (Exception e) {
                    logErros.add(new ErroImportacao(linhaNum, "import.error.generic", e.getMessage()));
                }
            }
        }

        return salvarEmLotes(leitoresParaSalvar, leitorRepository, "import.entity.readers", logErros, locale);
    }

    private Reader criarLeitorFromRow(Row row, Map<String, Integer> headerMap,
            Map<Integer, Course> courses,
            Map<Integer, StudyShift> studyShifts,
            Map<Integer, AcademicModule> academicModules,
            LibraryType libraryType,
            Locale locale) {
        Reader leitor = new Reader();
        leitor.setRegistrationNumber(ExcelUtils.getString(row.getCell(headerMap.get("matricula"))));
        leitor.setFullName(ExcelUtils.getString(row.getCell(headerMap.get("nome_completo"))));
        leitor.setCpf(normalizeNumber(ExcelUtils.getString(row.getCell(headerMap.get("cpf")))));
        leitor.setPhoneNumber(normalizeNumber(ExcelUtils.getString(row.getCell(headerMap.get("celular")))));
        leitor.setEmail(ExcelUtils.getString(row.getCell(headerMap.get("email"))));
        leitor.setBirthDate(ExcelUtils.getLocalDate(row.getCell(headerMap.get("data_nascimento"))));

        leitor.setPostalCode(normalizeNumber(ExcelUtils.getString(row.getCell(headerMap.get("cep")))));
        leitor.setStreet(ExcelUtils.getString(row.getCell(headerMap.get("logradouro"))));
        leitor.setDistrict(ExcelUtils.getString(row.getCell(headerMap.get("bairro"))));
        leitor.setCity(ExcelUtils.getString(row.getCell(headerMap.get("localidade"))));
        leitor.setStateCode(ExcelUtils.getString(row.getCell(headerMap.get("uf"))));
        leitor.setStreetNumber(ExcelUtils.getInteger(row.getCell(headerMap.get("numero_casa"))));
        leitor.setAddressComplement(ExcelUtils.getString(row.getCell(headerMap.get("complemento"))));

        Integer cursoId = ExcelUtils.getInteger(row.getCell(headerMap.get("curso_id")));
        Integer turnoId = ExcelUtils.getInteger(row.getCell(headerMap.get("turno_id")));
        Integer moduloId = ExcelUtils.getInteger(row.getCell(headerMap.get("modulo_id")));

        if (headerMap.containsKey("reader_category")) {
            leitor.setReaderCategory(ExcelUtils.getString(row.getCell(headerMap.get("reader_category"))));
        } else if (headerMap.containsKey("categoria")) {
            leitor.setReaderCategory(ExcelUtils.getString(row.getCell(headerMap.get("categoria"))));
        }

        if (libraryType == LibraryType.SCHOOL
                && (cursoId == null || turnoId == null || moduloId == null)) {
            throw new IllegalArgumentException(
                    messages.resolve("reader.academic-fields.required", locale));
        }

        if (cursoId != null && courses.containsKey(cursoId))
            leitor.setCourse(courses.get(cursoId));
        else if (libraryType == LibraryType.SCHOOL)
            throw new IllegalArgumentException(
                    messages.resolve("import.error.course.invalid", locale, cursoId));

        if (turnoId != null && studyShifts.containsKey(turnoId))
            leitor.setStudyShift(studyShifts.get(turnoId));
        else if (libraryType == LibraryType.SCHOOL)
            throw new IllegalArgumentException(
                    messages.resolve("metadata.study-shift.not-found", locale));

        if (moduloId != null && academicModules.containsKey(moduloId))
            leitor.setAcademicModule(academicModules.get(moduloId));
        else if (libraryType == LibraryType.SCHOOL)
            throw new IllegalArgumentException(
                    messages.resolve("metadata.academic-module.not-found", locale));

        return leitor;
    }

    // ===================== IMPORTAÇÃO DE LIVROS =====================

    @Transactional
    protected String importarLivros(MultipartFile file, Locale locale) throws Exception {
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
                            logErros.add(new ErroImportacao(linhaNum,
                                    "import.error.isbn.duplicate-in-sheet", isbn));
                            continue;
                        }
                        if (livroRepository.existsByIsbn(isbn)) {
                            logErros.add(new ErroImportacao(linhaNum,
                                    "import.error.isbn.already-exists", isbn));
                            continue;
                        }
                    }

                    Book livro = criarLivroFromRow(row, headerMap, locale);
                    livrosParaSalvar.add(livro);

                } catch (Exception e) {
                    logErros.add(new ErroImportacao(linhaNum, "import.error.generic", e.getMessage()));
                }
            }
        }
        return salvarEmLotes(livrosParaSalvar, livroRepository, "import.entity.books", logErros, locale);
    }

    private Book criarLivroFromRow(Row row, Map<String, Integer> headerMap, Locale locale) {
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
            throw new IllegalArgumentException(messages.resolve("import.error.dewey.required", locale));

        DeweyClassification cdd = deweyClassificationRepository.findById(cddCodigo)
                .orElseThrow(() -> new IllegalArgumentException(
                        messages.resolve("import.error.dewey.not-found", locale, cddCodigo)));
        livro.setDeweyClassification(cdd);

        livro.setGenres(new HashSet<>());

        return livro;
    }

    // ===================== IMPORTAÇÃO DE EXEMPLARES =====================

    @Transactional
    protected String importarExemplares(MultipartFile file, Locale locale) throws Exception {
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
                        logErros.add(new ErroImportacao(linhaNum, "import.error.copy-code.empty"));
                        continue;
                    }
                    if (!tombosNoExcel.add(tombo)) {
                        logErros.add(new ErroImportacao(linhaNum,
                                "import.error.copy-code.duplicate-in-sheet", tombo));
                        continue;
                    }
                    if (exemplarRepository.existsByCopyCode(tombo)) {
                        logErros.add(new ErroImportacao(linhaNum,
                                "import.error.copy-code.already-exists", tombo));
                        continue;
                    }

                    BookCopy exemplar = criarExemplarFromRow(row, headerMap, locale);
                    exemplaresParaSalvar.add(exemplar);

                } catch (Exception e) {
                    logErros.add(new ErroImportacao(linhaNum, "import.error.generic", e.getMessage()));
                }
            }
        }

        return salvarEmLotes(exemplaresParaSalvar, exemplarRepository, "import.entity.copies", logErros, locale);
    }

    private BookCopy criarExemplarFromRow(Row row, Map<String, Integer> headerMap, Locale locale) {
        String livroIdStr = ExcelUtils.getString(row.getCell(headerMap.get("livro_id")));
        if (livroIdStr == null || livroIdStr.isBlank())
            throw new IllegalArgumentException(messages.resolve("import.error.book-id.required", locale));

        UUID livroId;
        try {
            livroId = UUID.fromString(livroIdStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    messages.resolve("import.error.book-id.invalid-uuid", locale, livroIdStr));
        }

        Book livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new IllegalArgumentException(
                        messages.resolve("import.error.book.not-found", locale, livroId)));

        BookCopy exemplar = new BookCopy();
        exemplar.setCopyCode(ExcelUtils.getString(row.getCell(headerMap.get("tombo"))));
        exemplar.setShelfLocation(ExcelUtils.getString(row.getCell(headerMap.get("localizacao_fisica"))));
        exemplar.setStatus(ExcelUtils.getEnum(row.getCell(headerMap.get("status_livro")), BookCopyStatus.class,
                BookCopyStatus.AVAILABLE));
        exemplar.setBook(livro);

        return exemplar;
    }

    // ===================== UTILITÁRIOS =====================

    private <T> String salvarEmLotes(List<T> lista, JpaRepository<T, ?> repository, String entityKey,
            List<ErroImportacao> erros, Locale locale) {
        int salvos = 0;
        for (int i = 0; i < lista.size(); i += BATCH_SIZE) {
            int fim = Math.min(i + BATCH_SIZE, lista.size());
            List<T> lote = lista.subList(i, fim);
            try {
                repository.saveAll(lote);
                salvos += lote.size();
            } catch (Exception e) {
                erros.add(new ErroImportacao(-1, "import.error.batch-save-failed",
                        i / BATCH_SIZE + 1, e.getMessage()));
                log.error("Erro ao salvar lote de {}", entityKey, e);
            }
        }
        return gerarResumo(entityKey, salvos, erros, locale);
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

    private void validarArquivo(MultipartFile file, Locale locale) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException(messages.resolve("import.error.file.empty", locale));
        if (!Objects.equals(file.getContentType(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            throw new IllegalArgumentException(messages.resolve("import.error.file.invalid-format", locale));
        }
    }

    private String normalizeNumber(String val) {
        return val == null ? null : val.replaceAll("\\D", "");
    }

    private String gerarResumo(String entityKey, int salvos, List<ErroImportacao> erros, Locale locale) {
        String entityLabel = messages.resolve(entityKey, locale);
        StringBuilder sb = new StringBuilder();
        sb.append(messages.resolve("email.import.summary", locale, entityLabel, salvos, erros.size()));
        if (!erros.isEmpty()) {
            sb.append(" Detalhes: ")
                    .append(erros.stream().limit(5)
                            .map(e -> e.toLocalizedString(messages, locale))
                            .collect(Collectors.joining("; ")));
            if (erros.size() > 5)
                sb.append("...");
        }
        return sb.toString();
    }

    private record ErroImportacao(int linha, String messageKey, Object... args) {
        String toLocalizedString(MessageResolver resolver, Locale locale) {
            String body = resolver.resolve(messageKey, locale, args);
            return linha > 0 ? resolver.resolve("import.error.line", locale, linha, body) : body;
        }
    }
}
