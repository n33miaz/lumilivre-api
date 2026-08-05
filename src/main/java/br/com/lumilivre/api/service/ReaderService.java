package br.com.lumilivre.api.service;

import static br.com.lumilivre.api.config.CacheNames.READER_COUNT;

import java.time.LocalDate;
import java.util.List;

import java.util.Locale;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.lumilivre.api.dto.reader.ReaderListItem;
import br.com.lumilivre.api.dto.reader.ReaderRankingItem;
import br.com.lumilivre.api.dto.reader.ReaderRequest;
import br.com.lumilivre.api.enums.LibraryType;
import br.com.lumilivre.api.enums.PenaltyCode;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.model.AcademicModule;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.model.Course;
import br.com.lumilivre.api.model.Reader;
import br.com.lumilivre.api.model.StudyShift;
import br.com.lumilivre.api.repository.AcademicModuleRepository;
import br.com.lumilivre.api.repository.AppUserRepository;
import br.com.lumilivre.api.repository.CourseRepository;
import br.com.lumilivre.api.repository.ReaderRepository;
import br.com.lumilivre.api.repository.StudyShiftRepository;
import br.com.lumilivre.api.security.Auditable;
import br.com.lumilivre.api.security.CustomUserDetails;
import br.com.lumilivre.api.service.infra.EmailService;
import br.com.lumilivre.api.service.infra.postalcode.PostalAddress;
import br.com.lumilivre.api.service.infra.postalcode.PostalCodeRouter;
import br.com.lumilivre.api.service.infra.storage.StorageBucket;
import br.com.lumilivre.api.service.infra.storage.StorageProvider;

@Service
public class ReaderService {

    private final ReaderRepository readerRepository;
    private final CourseRepository courseRepository;
    private final AppUserRepository appUserRepository;
    private final StudyShiftRepository studyShiftRepository;
    private final AcademicModuleRepository academicModuleRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final PostalCodeRouter postalCodeRouter;
    private final StorageProvider storageProvider;
    private final SettingsService settingsService;

    private record RelatedEntities(Course course, StudyShift studyShift, AcademicModule academicModule) {
    }

    public ReaderService(ReaderRepository readerRepository, CourseRepository courseRepository,
            AppUserRepository appUserRepository, StudyShiftRepository studyShiftRepository,
            AcademicModuleRepository academicModuleRepository, EmailService emailService,
            PasswordEncoder passwordEncoder, PostalCodeRouter postalCodeRouter, StorageProvider storageProvider,
            SettingsService settingsService) {
        this.readerRepository = readerRepository;
        this.courseRepository = courseRepository;
        this.appUserRepository = appUserRepository;
        this.studyShiftRepository = studyShiftRepository;
        this.academicModuleRepository = academicModuleRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.postalCodeRouter = postalCodeRouter;
        this.storageProvider = storageProvider;
        this.settingsService = settingsService;
    }

    public Page<ReaderListItem> listarParaAdminV2(String texto, Pageable pageable) {
        if (texto == null || texto.isBlank()) {
            return readerRepository.findReaderListItems(pageable);
        }
        return readerRepository.findReaderListItemsByText(texto, pageable);
    }

    public Page<ReaderListItem> buscarAvancadoV2(String penalidadeStr, String matricula, String nome,
            String cursoNome, Integer turnoId, Integer moduloId, LocalDate dataNascimento,
            String email, String celular, Pageable pageable) {
        PenaltyCode penalidadeEnum = parseEnum(penalidadeStr, PenaltyCode.class);
        String nomeFiltro = criarFiltroLike(nome);
        String cursoNomeFiltro = criarFiltroLike(cursoNome);
        String emailFiltro = criarFiltroLike(email);

        return readerRepository.buscarAvancadoV2(
                penalidadeEnum, matricula, nomeFiltro, cursoNomeFiltro, turnoId, moduloId, dataNascimento,
                emailFiltro, celular, pageable);
    }

    public Reader buscarPorMatricula(String matricula) {
        return readerRepository.findByRegistrationNumber(matricula)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("reader.not-found"));
    }

    @Cacheable(value = READER_COUNT)
    public long getContagemTotal() {
        return readerRepository.count();
    }

    @Auditable(action = "READER_CREATED", targetParam = "#request.registrationNumber")
    @Transactional
    @CacheEvict(value = READER_COUNT, allEntries = true)
    public Reader cadastrar(ReaderRequest request) {
        if (readerRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw BusinessRuleException.ofKey("reader.registration.already-registered");
        }

        if (request.getCpf() != null && !request.getCpf().isBlank()
                && readerRepository.existsByCpf(request.getCpf())) {
            throw BusinessRuleException.ofKey("reader.cpf.already-registered");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()
                && appUserRepository.existsByEmail(request.getEmail())) {
            throw BusinessRuleException.ofKey("reader.email.already-in-use");
        }

        RelatedEntities relatedEntities = buscarEntidadesRelacionadas(request);

        Reader reader = new Reader();
        mapearDtoParaEntidade(reader, request, relatedEntities);
        preencherEnderecoPorCep(reader, request.getPostalCode());

        if (reader.getEmail() != null && !reader.getEmail().isBlank()) {
            AppUser appUser = criarUsuarioParaLeitor(reader);
            reader.setAppUser(appUser);
        }

        Reader savedReader = readerRepository.save(reader);

        if (reader.getEmail() != null && !reader.getEmail().isBlank()) {
            try {
                Locale locale = LocaleContextHolder.getLocale();
                emailService.enviarSenhaInicial(
                        reader.getEmail(), reader.getFullName(), request.getRegistrationNumber(), locale);
            } catch (Exception e) {
                System.err.println("Erro ao enviar email: " + e.getMessage());
            }
        }

        return savedReader;
    }

    @Auditable(action = "READER_UPDATED", targetParam = "#matricula")
    @Transactional
    public Reader atualizar(String matricula, ReaderRequest request) {
        Reader reader = readerRepository.findByRegistrationNumber(matricula)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("reader.not-found"));

        if (request.getCpf() != null && !request.getCpf().isBlank()) {
            boolean cpfMudou = reader.getCpf() == null || !reader.getCpf().equals(request.getCpf());
            if (cpfMudou && readerRepository.existsByCpf(request.getCpf())) {
                throw BusinessRuleException.ofKey("reader.cpf.already-in-use-by-other");
            }
        }

        RelatedEntities relatedEntities = buscarEntidadesRelacionadas(request);

        mapearDtoParaEntidade(reader, request, relatedEntities);
        preencherEnderecoPorCep(reader, request.getPostalCode());

        // NÃO resetar a senha ao editar o CPF (era um vetor de senha previsível).

        if (reader.getAppUser() != null && !reader.getEmail().equals(reader.getAppUser().getEmail())) {
            reader.getAppUser().setEmail(reader.getEmail());
        }

        return readerRepository.save(reader);
    }

    @Auditable(action = "READER_DELETED", targetParam = "#matricula")
    @Transactional
    @CacheEvict(value = READER_COUNT, allEntries = true)
    public void excluir(String matricula) {
        Reader reader = readerRepository.findByRegistrationNumber(matricula)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("reader.not-found"));

        if (reader.getAppUser() != null) {
            appUserRepository.delete(reader.getAppUser());
        }
        readerRepository.delete(reader);
    }

    @Auditable(action = "READER_PASSWORD_RESET", targetParam = "#matricula")
    @Transactional
    public void resetarSenha(String matricula) {
        Reader reader = readerRepository.findByRegistrationNumber(matricula)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("reader.not-found"));

        if (reader.getAppUser() == null) {
            throw BusinessRuleException.ofKey("reader.no-app-user-linked");
        }

        reader.getAppUser().setPasswordHash(passwordEncoder.encode(reader.getRegistrationNumber()));
        // Reset volta para a matrícula → força troca no próximo login.
        reader.getAppUser().setMustChangePassword(true);
        appUserRepository.save(reader.getAppUser());
    }

    // A foto e imagem de uma pessoa e a troca e governada por flag global
    // (reader_can_edit_avatar); auditar e o que torna a flag verificavel depois.
    @Auditable(action = "READER_AVATAR_UPDATED", targetParam = "#matricula")
    @Transactional
    public void uploadFoto(String matricula, MultipartFile file) {
        // Defesa em profundidade: quando a permissão global está desligada,
        // um LEITOR não pode trocar a própria foto pelo app; ADMIN/BIBLIOTECARIO sempre podem.
        if (isSelfServiceReader() && !settingsService.isReaderCanEditAvatar()) {
            throw new AccessDeniedException("reader.avatar.edit-not-allowed");
        }

        Reader reader = readerRepository.findByRegistrationNumber(matricula)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("reader.not-found"));

        try {
            String url = storageProvider.upload(file, StorageBucket.AVATARS);
            reader.setAvatarUrl(url);
            readerRepository.save(reader);
        } catch (Exception e) {
            throw BusinessRuleException.ofKey("reader.avatar.upload-failed");
        }
    }

    public List<ReaderRankingItem> gerarRankingLeitoresV2(int top, Integer cursoId, Integer moduloId, Integer turnoId) {
        return readerRepository.findRankingItems(cursoId, moduloId, turnoId, org.springframework.data.domain.PageRequest.of(0, top))
                .getContent();
    }

    private RelatedEntities buscarEntidadesRelacionadas(ReaderRequest request) {
        if (settingsService.getLibraryType() == LibraryType.SCHOOL) {
            if (request.getCourseId() == null || request.getStudyShiftId() == null
                    || request.getAcademicModuleId() == null) {
                throw BusinessRuleException.ofKey("reader.academic-fields.required");
            }
        }

        Course course = request.getCourseId() == null ? null : courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> ResourceNotFoundException.ofKey("course.not-found-with-id", request.getCourseId()));

        StudyShift studyShift = request.getStudyShiftId() == null ? null : studyShiftRepository.findById(request.getStudyShiftId())
                .orElseThrow(() -> ResourceNotFoundException.ofKey("metadata.study-shift.not-found"));

        AcademicModule academicModule = request.getAcademicModuleId() == null ? null : academicModuleRepository.findById(request.getAcademicModuleId())
                .orElseThrow(() -> ResourceNotFoundException.ofKey("metadata.academic-module.not-found"));

        return new RelatedEntities(course, studyShift, academicModule);
    }

    private void mapearDtoParaEntidade(Reader reader, ReaderRequest request, RelatedEntities relatedEntities) {
        if (request.getRegistrationNumber() != null) {
            reader.setRegistrationNumber(request.getRegistrationNumber());
        }
        reader.setFullName(request.getFullName());
        reader.setCpf(request.getCpf());
        reader.setBirthDate(request.getBirthDate());
        reader.setPhoneNumber(request.getPhoneNumber());
        reader.setEmail(request.getEmail());
        reader.setStreetNumber(request.getStreetNumber());
        reader.setAddressComplement(request.getAddressComplement());
        reader.setCourse(relatedEntities.course());
        reader.setStudyShift(relatedEntities.studyShift());
        reader.setAcademicModule(relatedEntities.academicModule());
        reader.setReaderCategory(request.getReaderCategory());

        if (request.getPenaltyCode() != null) {
            if (request.getPenaltyCode().isBlank()) {
                reader.setPenaltyCode(null);
            } else {
                reader.setPenaltyCode(parseEnum(request.getPenaltyCode(), PenaltyCode.class));
            }
        }
    }

    private void preencherEnderecoPorCep(Reader reader, String cep) {
        if (cep == null || cep.isBlank()) {
            return;
        }
        String cepLimpo = cep.replace("-", "").trim();
        if (cepLimpo.length() != 8) {
            return;
        }
        try {
            postalCodeRouter.lookup(cepLimpo, "BR").ifPresent(address -> applyAddress(reader, address));
        } catch (Exception e) {
            // falha silenciosa: o cadastro continua sem autofill
        }
    }

    private void applyAddress(Reader reader, PostalAddress address) {
        if (address.street() == null) return;
        reader.setPostalCode(address.postalCode());
        reader.setStreet(address.street());
        reader.setCity(address.city());
        reader.setDistrict(address.district());
        reader.setStateCode(address.regionCode());
    }

    private AppUser criarUsuarioParaLeitor(Reader reader) {
        AppUser appUser = new AppUser();
        appUser.setEmail(reader.getEmail());
        appUser.setPasswordHash(passwordEncoder.encode(reader.getRegistrationNumber()));
        appUser.setRole(Role.READER);
        appUser.setReader(reader);
        // Senha inicial = matrícula (previsível) → força troca no primeiro acesso.
        appUser.setMustChangePassword(true);
        return appUser;
    }

    /** True quando o chamador autenticado é um LEITOR (self-service pelo app). */
    private boolean isSelfServiceReader() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null
                && auth.getPrincipal() instanceof CustomUserDetails details
                && details.getAppUser().getRole() == Role.READER;
    }

    private <T extends Enum<T>> T parseEnum(String value, Class<T> enumClass) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String criarFiltroLike(String valor) {
        return (valor != null && !valor.isBlank()) ? "%" + valor + "%" : null;
    }
}
