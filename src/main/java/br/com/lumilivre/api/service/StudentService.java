package br.com.lumilivre.api.service;

import static br.com.lumilivre.api.config.CacheNames.STUDENT_COUNT;

import java.time.LocalDate;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.lumilivre.api.dto.common.AddressLookupResponse;
import br.com.lumilivre.api.dto.student.StudentListItem;
import br.com.lumilivre.api.dto.student.StudentRankingItem;
import br.com.lumilivre.api.dto.student.StudentRequest;
import br.com.lumilivre.api.enums.PenaltyCode;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.model.AcademicModule;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.model.Course;
import br.com.lumilivre.api.model.Student;
import br.com.lumilivre.api.model.StudyShift;
import br.com.lumilivre.api.repository.AcademicModuleRepository;
import br.com.lumilivre.api.repository.AppUserRepository;
import br.com.lumilivre.api.repository.CourseRepository;
import br.com.lumilivre.api.repository.StudentRepository;
import br.com.lumilivre.api.repository.StudyShiftRepository;
import br.com.lumilivre.api.service.infra.CepService;
import br.com.lumilivre.api.service.infra.EmailService;
import br.com.lumilivre.api.service.infra.SupabaseStorageService;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final AppUserRepository appUserRepository;
    private final StudyShiftRepository studyShiftRepository;
    private final AcademicModuleRepository academicModuleRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final CepService cepService;
    private final SupabaseStorageService storageService;

    private record RelatedEntities(Course course, StudyShift studyShift, AcademicModule academicModule) {
    }

    public StudentService(StudentRepository studentRepository, CourseRepository courseRepository,
            AppUserRepository appUserRepository, StudyShiftRepository studyShiftRepository,
            AcademicModuleRepository academicModuleRepository, EmailService emailService,
            PasswordEncoder passwordEncoder, CepService cepService, SupabaseStorageService supabaseStorageService) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.appUserRepository = appUserRepository;
        this.studyShiftRepository = studyShiftRepository;
        this.academicModuleRepository = academicModuleRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.cepService = cepService;
        this.storageService = supabaseStorageService;
    }

    public Page<StudentListItem> listarParaAdminV2(String texto, Pageable pageable) {
        if (texto == null || texto.isBlank()) {
            return studentRepository.findStudentListItems(pageable);
        }
        return studentRepository.findStudentListItemsByText(texto, pageable);
    }

    public Page<StudentListItem> buscarAvancadoV2(String penalidadeStr, String matricula, String nome,
            String cursoNome, Integer turnoId, Integer moduloId, LocalDate dataNascimento,
            String email, String celular, Pageable pageable) {
        PenaltyCode penalidadeEnum = parseEnum(penalidadeStr, PenaltyCode.class);
        String nomeFiltro = criarFiltroLike(nome);
        String cursoNomeFiltro = criarFiltroLike(cursoNome);
        String emailFiltro = criarFiltroLike(email);

        return studentRepository.buscarAvancadoV2(
                penalidadeEnum, matricula, nomeFiltro, cursoNomeFiltro, turnoId, moduloId, dataNascimento,
                emailFiltro, celular, pageable);
    }

    public Student buscarPorMatricula(String matricula) {
        return studentRepository.findByRegistrationNumber(matricula)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("student.not-found"));
    }

    @Cacheable(value = STUDENT_COUNT)
    public long getContagemTotal() {
        return studentRepository.count();
    }

    @Transactional
    @CacheEvict(value = STUDENT_COUNT, allEntries = true)
    public Student cadastrar(StudentRequest request) {
        if (studentRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw BusinessRuleException.ofKey("student.registration.already-registered");
        }

        if (request.getCpf() != null && !request.getCpf().isBlank()
                && studentRepository.existsByCpf(request.getCpf())) {
            throw BusinessRuleException.ofKey("student.cpf.already-registered");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()
                && appUserRepository.existsByEmail(request.getEmail())) {
            throw BusinessRuleException.ofKey("student.email.already-in-use");
        }

        RelatedEntities relatedEntities = buscarEntidadesRelacionadas(request);

        Student student = new Student();
        mapearDtoParaEntidade(student, request, relatedEntities);
        preencherEnderecoPorCep(student, request.getPostalCode());

        if (student.getEmail() != null && !student.getEmail().isBlank()) {
            AppUser appUser = criarUsuarioParaAluno(student);
            student.setAppUser(appUser);
        }

        Student savedStudent = studentRepository.save(student);

        if (student.getEmail() != null && !student.getEmail().isBlank()) {
            try {
                emailService.enviarSenhaInicial(
                        student.getEmail(), student.getFullName(), request.getRegistrationNumber());
            } catch (Exception e) {
                System.err.println("Erro ao enviar email: " + e.getMessage());
            }
        }

        return savedStudent;
    }

    @Transactional
    public Student atualizar(String matricula, StudentRequest request) {
        Student student = studentRepository.findByRegistrationNumber(matricula)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("student.not-found"));

        if (request.getCpf() != null && !request.getCpf().isBlank()) {
            boolean cpfMudou = student.getCpf() == null || !student.getCpf().equals(request.getCpf());
            if (cpfMudou && studentRepository.existsByCpf(request.getCpf())) {
                throw BusinessRuleException.ofKey("student.cpf.already-in-use-by-other");
            }
        }

        RelatedEntities relatedEntities = buscarEntidadesRelacionadas(request);

        boolean cpfMudou = request.getCpf() != null
                && !request.getCpf().isBlank()
                && (student.getCpf() == null || !student.getCpf().equals(request.getCpf()));

        mapearDtoParaEntidade(student, request, relatedEntities);
        preencherEnderecoPorCep(student, request.getPostalCode());

        if (cpfMudou && student.getAppUser() != null && request.getCpf() != null) {
            student.getAppUser().setPasswordHash(passwordEncoder.encode(request.getCpf()));
        }

        if (student.getAppUser() != null && !student.getEmail().equals(student.getAppUser().getEmail())) {
            student.getAppUser().setEmail(student.getEmail());
        }

        return studentRepository.save(student);
    }

    @Transactional
    @CacheEvict(value = STUDENT_COUNT, allEntries = true)
    public void excluir(String matricula) {
        Student student = studentRepository.findByRegistrationNumber(matricula)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("student.not-found"));

        if (student.getAppUser() != null) {
            appUserRepository.delete(student.getAppUser());
        }
        studentRepository.delete(student);
    }

    @Transactional
    public void resetarSenha(String matricula) {
        Student student = studentRepository.findByRegistrationNumber(matricula)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("student.not-found"));

        if (student.getAppUser() == null) {
            throw BusinessRuleException.ofKey("student.no-app-user-linked");
        }

        student.getAppUser().setPasswordHash(passwordEncoder.encode(student.getRegistrationNumber()));
        appUserRepository.save(student.getAppUser());
    }

    @Transactional
    public void uploadFoto(String matricula, MultipartFile file) {
        Student student = studentRepository.findByRegistrationNumber(matricula)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("student.not-found"));

        try {
            String url = storageService.uploadFile(file, "avatars");
            student.setAvatarUrl(url);
            studentRepository.save(student);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao enviar foto de perfil: " + e.getMessage());
        }
    }

    public List<StudentRankingItem> gerarRankingAlunosV2(int top, Integer cursoId, Integer moduloId, Integer turnoId) {
        return studentRepository.findRankingItems(cursoId, moduloId, turnoId, org.springframework.data.domain.PageRequest.of(0, top))
                .getContent();
    }

    private RelatedEntities buscarEntidadesRelacionadas(StudentRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> ResourceNotFoundException.ofKey("course.not-found-with-id", request.getCourseId()));

        StudyShift studyShift = studyShiftRepository.findById(request.getStudyShiftId())
                .orElseThrow(() -> ResourceNotFoundException.ofKey("metadata.study-shift.not-found"));

        AcademicModule academicModule = academicModuleRepository.findById(request.getAcademicModuleId())
                .orElseThrow(() -> ResourceNotFoundException.ofKey("metadata.academic-module.not-found"));

        return new RelatedEntities(course, studyShift, academicModule);
    }

    private void mapearDtoParaEntidade(Student student, StudentRequest request, RelatedEntities relatedEntities) {
        if (request.getRegistrationNumber() != null) {
            student.setRegistrationNumber(request.getRegistrationNumber());
        }
        student.setFullName(request.getFullName());
        student.setCpf(request.getCpf());
        student.setBirthDate(request.getBirthDate());
        student.setPhoneNumber(request.getPhoneNumber());
        student.setEmail(request.getEmail());
        student.setStreetNumber(request.getStreetNumber());
        student.setAddressComplement(request.getAddressComplement());
        student.setCourse(relatedEntities.course());
        student.setStudyShift(relatedEntities.studyShift());
        student.setAcademicModule(relatedEntities.academicModule());

        if (request.getPenaltyCode() != null) {
            if (request.getPenaltyCode().isBlank()) {
                student.setPenaltyCode(null);
            } else {
                student.setPenaltyCode(parseEnum(request.getPenaltyCode(), PenaltyCode.class));
            }
        }
    }

    private void preencherEnderecoPorCep(Student student, String cep) {
        if (cep != null && !cep.isBlank()) {
            String cepLimpo = cep.replace("-", "").trim();
            if (cepLimpo.length() != 8) {
                return;
            }

            try {
                AddressLookupResponse enderecoDTO = cepService.buscarEnderecoPorCep(cepLimpo);
                if (enderecoDTO != null && enderecoDTO.getLogradouro() != null) {
                    student.setPostalCode(cepLimpo);
                    student.setStreet(enderecoDTO.getLogradouro());
                    student.setCity(enderecoDTO.getLocalidade());
                    student.setDistrict(enderecoDTO.getBairro());
                    student.setStateCode(enderecoDTO.getUf());
                }
            } catch (Exception e) {
                // falha silenciosa
            }
        }
    }

    private AppUser criarUsuarioParaAluno(Student student) {
        AppUser appUser = new AppUser();
        appUser.setEmail(student.getEmail());
        appUser.setPasswordHash(passwordEncoder.encode(student.getRegistrationNumber()));
        appUser.setRole(Role.STUDENT);
        appUser.setStudent(student);
        return appUser;
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
