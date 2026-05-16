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
import br.com.lumilivre.api.dto.v1.aluno.AlunoRequest;
import br.com.lumilivre.api.dto.v1.aluno.AlunoResumoResponse;
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

    public Page<AlunoResumoResponse> buscarAlunosParaListaAdmin(String texto, Pageable pageable) {
        if (texto != null && !texto.isBlank()) {
            return studentRepository.findAlunosParaListaAdminComFiltro(texto, pageable);
        }
        return studentRepository.findAlunosParaListaAdmin(pageable);
    }

    public Page<AlunoResumoResponse> buscarPorTexto(String texto, Pageable pageable) {
        if (texto == null || texto.isBlank()) {
            return studentRepository.findAlunosParaListaAdmin(pageable);
        }
        return studentRepository.findAlunosParaListaAdminComFiltro(texto, pageable);
    }

    public Page<StudentListItem> listarParaAdminV2(String texto, Pageable pageable) {
        if (texto == null || texto.isBlank()) {
            return studentRepository.findStudentListItems(pageable);
        }
        return studentRepository.findStudentListItemsByText(texto, pageable);
    }

    public Page<AlunoResumoResponse> buscarAvancado(String penalidadeStr, String matricula, String nome,
            String cursoNome, Integer turnoId, Integer moduloId, LocalDate dataNascimento,
            String email, String celular, Pageable pageable) {
        PenaltyCode penalidadeEnum = parseEnum(penalidadeStr, PenaltyCode.class);
        String nomeFiltro = criarFiltroLike(nome);
        String cursoNomeFiltro = criarFiltroLike(cursoNome);
        String emailFiltro = criarFiltroLike(email);

        return studentRepository.buscarAvancadoComDTO(
                penalidadeEnum, matricula, nomeFiltro, cursoNomeFiltro, turnoId, moduloId, dataNascimento,
                emailFiltro, celular, pageable);
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
    public Student cadastrar(AlunoRequest dto) {
        if (studentRepository.existsByRegistrationNumber(dto.getMatricula())) {
            throw BusinessRuleException.ofKey("student.registration.already-registered");
        }

        if (dto.getCpf() != null && !dto.getCpf().isBlank() && studentRepository.existsByCpf(dto.getCpf())) {
            throw BusinessRuleException.ofKey("student.cpf.already-registered");
        }

        if (dto.getEmail() != null && !dto.getEmail().isBlank() && appUserRepository.existsByEmail(dto.getEmail())) {
            throw BusinessRuleException.ofKey("student.email.already-in-use");
        }

        RelatedEntities relatedEntities = buscarEntidadesRelacionadas(dto);

        Student student = new Student();
        mapearDtoParaEntidade(student, dto, relatedEntities);
        preencherEnderecoPorCep(student, dto.getCep());

        if (student.getEmail() != null && !student.getEmail().isBlank()) {
            AppUser appUser = criarUsuarioParaAluno(student);
            student.setAppUser(appUser);
        }

        Student savedStudent = studentRepository.save(student);

        if (student.getEmail() != null && !student.getEmail().isBlank()) {
            try {
                emailService.enviarSenhaInicial(student.getEmail(), student.getFullName(), dto.getMatricula());
            } catch (Exception e) {
                System.err.println("Erro ao enviar email: " + e.getMessage());
            }
        }

        return savedStudent;
    }

    @Transactional
    @CacheEvict(value = STUDENT_COUNT, allEntries = true)
    public Student cadastrar(StudentRequest request) {
        return cadastrar(AlunoRequest.builder()
                .matricula(request.getRegistrationNumber())
                .nomeCompleto(request.getFullName())
                .cpf(request.getCpf())
                .dataNascimento(request.getBirthDate())
                .celular(request.getPhoneNumber())
                .email(request.getEmail())
                .cursoId(request.getCourseId())
                .turnoId(request.getStudyShiftId())
                .moduloId(request.getAcademicModuleId())
                .cep(request.getPostalCode())
                .logradouro(request.getStreet())
                .complemento(request.getAddressComplement())
                .localidade(request.getCity())
                .bairro(request.getDistrict())
                .uf(request.getStateCode())
                .numeroCasa(request.getStreetNumber())
                .penalidade(request.getPenaltyCode())
                .build());
    }

    @Transactional
    public Student atualizar(String matricula, AlunoRequest dto) {
        Student student = studentRepository.findByRegistrationNumber(matricula)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("student.not-found"));

        if (dto.getCpf() != null && !dto.getCpf().isBlank()) {
            boolean cpfMudou = student.getCpf() == null || !student.getCpf().equals(dto.getCpf());
            if (cpfMudou && studentRepository.existsByCpf(dto.getCpf())) {
                throw BusinessRuleException.ofKey("student.cpf.already-in-use-by-other");
            }
        }

        RelatedEntities relatedEntities = buscarEntidadesRelacionadas(dto);

        boolean cpfMudou = dto.getCpf() != null
                && !dto.getCpf().isBlank()
                && (student.getCpf() == null || !student.getCpf().equals(dto.getCpf()));

        mapearDtoParaEntidade(student, dto, relatedEntities);
        preencherEnderecoPorCep(student, dto.getCep());

        if (cpfMudou && student.getAppUser() != null && dto.getCpf() != null) {
            student.getAppUser().setPasswordHash(passwordEncoder.encode(dto.getCpf()));
        }

        if (student.getAppUser() != null && !student.getEmail().equals(student.getAppUser().getEmail())) {
            student.getAppUser().setEmail(student.getEmail());
        }

        return studentRepository.save(student);
    }

    @Transactional
    public Student atualizar(String matricula, StudentRequest request) {
        return atualizar(matricula, AlunoRequest.builder()
                .matricula(request.getRegistrationNumber())
                .nomeCompleto(request.getFullName())
                .cpf(request.getCpf())
                .dataNascimento(request.getBirthDate())
                .celular(request.getPhoneNumber())
                .email(request.getEmail())
                .cursoId(request.getCourseId())
                .turnoId(request.getStudyShiftId())
                .moduloId(request.getAcademicModuleId())
                .cep(request.getPostalCode())
                .logradouro(request.getStreet())
                .complemento(request.getAddressComplement())
                .localidade(request.getCity())
                .bairro(request.getDistrict())
                .uf(request.getStateCode())
                .numeroCasa(request.getStreetNumber())
                .penalidade(request.getPenaltyCode())
                .build());
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

    private RelatedEntities buscarEntidadesRelacionadas(AlunoRequest dto) {
        Course course = courseRepository.findById(dto.getCursoId())
                .orElseThrow(() -> new ResourceNotFoundException("Curso não encontrado (ID: " + dto.getCursoId() + ")"));

        StudyShift studyShift = studyShiftRepository.findById(dto.getTurnoId())
                .orElseThrow(() -> new ResourceNotFoundException("Turno não encontrado (ID: " + dto.getTurnoId() + ")"));

        AcademicModule academicModule = academicModuleRepository.findById(dto.getModuloId())
                .orElseThrow(() -> new ResourceNotFoundException("Módulo não encontrado (ID: " + dto.getModuloId() + ")"));

        return new RelatedEntities(course, studyShift, academicModule);
    }

    private void mapearDtoParaEntidade(Student student, AlunoRequest dto, RelatedEntities relatedEntities) {
        if (dto.getMatricula() != null) {
            student.setRegistrationNumber(dto.getMatricula());
        }
        student.setFullName(dto.getNomeCompleto());
        student.setCpf(dto.getCpf());
        student.setBirthDate(dto.getDataNascimento());
        student.setPhoneNumber(dto.getCelular());
        student.setEmail(dto.getEmail());
        student.setStreetNumber(dto.getNumeroCasa());
        student.setAddressComplement(dto.getComplemento());
        student.setCourse(relatedEntities.course());
        student.setStudyShift(relatedEntities.studyShift());
        student.setAcademicModule(relatedEntities.academicModule());

        if (dto.getPenalidade() != null) {
            if (dto.getPenalidade().isBlank()) {
                student.setPenaltyCode(null);
            } else {
                student.setPenaltyCode(parseEnum(dto.getPenalidade(), PenaltyCode.class));
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
