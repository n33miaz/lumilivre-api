package br.com.lumilivre.api.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.data.AlunoDTO;
import br.com.lumilivre.api.data.ListaAlunoDTO;
import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.enums.Turno;
import br.com.lumilivre.api.model.AlunoModel;
import br.com.lumilivre.api.model.CursoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.model.UsuarioModel;
import br.com.lumilivre.api.repository.AlunoRepository;
import br.com.lumilivre.api.repository.CursoRepository;
import br.com.lumilivre.api.repository.UsuarioRepository;
import br.com.lumilivre.api.utils.CpfValidator;

@Service
public class AlunoService {

    private final AlunoRepository ar;
    private final CursoRepository cursoRepository;
    private final UsuarioRepository ur;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final CepService cepService;

    // Injeção de dependência via construtor
    @Autowired
    public AlunoService(AlunoRepository ar, CursoRepository cursoRepository, UsuarioRepository ur,
            EmailService emailService, PasswordEncoder passwordEncoder, CepService cepService) {
        this.ar = ar;
        this.cursoRepository = cursoRepository;
        this.ur = ur;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.cepService = cepService;
    }

    public Page<ListaAlunoDTO> buscarAlunosParaListaAdmin(String texto, Pageable pageable) {
        if (texto != null && !texto.isBlank()) {
            return ar.findAlunosParaListaAdminComFiltro(texto, pageable);
        } else {
            return ar.findAlunosParaListaAdmin(pageable);
        }
    }

    public Page<AlunoModel> buscarPorTexto(String texto, Pageable pageable) {
        if (texto == null || texto.isBlank()) {
            return ar.findAll(pageable);
        }
        return ar.buscarPorTexto(texto, pageable);
    }

    public Page<AlunoModel> buscarAvancado(String penalidadeStr, String matricula, String nome,
            String cursoNome, String turnoStr, String modulo, LocalDate dataNascimento,
            String email, String celular, Pageable pageable) {

        Penalidade penalidadeEnum = null;
        if (penalidadeStr != null && !penalidadeStr.isBlank()) {
            try {
                penalidadeEnum = Penalidade.valueOf(penalidadeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignora se o valor for inválido
            }
        }

        Turno turnoEnum = null;
        if (turnoStr != null && !turnoStr.isBlank()) {
            try {
                turnoEnum = Turno.valueOf(turnoStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignora se o valor for inválido
            }
        }

        String nomeFiltro = (nome != null && !nome.isBlank()) ? "%" + nome + "%" : null;
        String cursoNomeFiltro = (cursoNome != null && !cursoNome.isBlank()) ? "%" + cursoNome + "%" : null;
        String moduloFiltro = (modulo != null && !modulo.isBlank()) ? "%" + modulo + "%" : null;
        String emailFiltro = (email != null && !email.isBlank()) ? "%" + email + "%" : null;

        return ar.buscarAvancado(
                penalidadeEnum, matricula, nomeFiltro, cursoNomeFiltro, turnoEnum, moduloFiltro, dataNascimento,
                emailFiltro, celular, pageable);
    }

    @Transactional
    public ResponseEntity<?> cadastrar(AlunoDTO dto) {
        if (dto.getMatricula() == null || dto.getMatricula().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseModel("A matrícula é obrigatória."));
        }
        if (!dto.getMatricula().matches("\\d{5}")) {
            return ResponseEntity.badRequest().body(new ResponseModel("A matrícula deve conter 5 dígitos numéricos."));
        }
        if (ar.existsById(dto.getMatricula())) {
            return ResponseEntity.badRequest().body(new ResponseModel("Essa matrícula já está cadastrada."));
        }
        if (dto.getNomeCompleto() == null || dto.getNomeCompleto().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseModel("O nome completo é obrigatório."));
        }
        if (dto.getCpf() == null || dto.getCpf().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseModel("O CPF é obrigatório."));
        }
        if (!CpfValidator.isCpfValido(dto.getCpf())) {
            return ResponseEntity.badRequest().body(new ResponseModel("CPF inválido."));
        }
        if (ar.existsByCpf(dto.getCpf())) {
            return ResponseEntity.badRequest().body(new ResponseModel("CPF já cadastrado."));
        }
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseModel("O email é obrigatório."));
        }
        if (dto.getCelular() == null || dto.getCelular().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseModel("O celular é obrigatório."));
        }
        if (dto.getCursoId() == null) {
            return ResponseEntity.badRequest().body(new ResponseModel("O curso é obrigatório."));
        }

        Optional<CursoModel> cursoOpt = cursoRepository.findById(dto.getCursoId());
        if (cursoOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseModel("Curso não encontrado."));
        }

        AlunoModel aluno = new AlunoModel();
        aluno.setMatricula(dto.getMatricula());
        aluno.setNomeCompleto(dto.getNomeCompleto());
        aluno.setCpf(dto.getCpf());
        aluno.setDataNascimento(dto.getDataNascimento());
        aluno.setCelular(dto.getCelular());
        aluno.setEmail(dto.getEmail());
        aluno.setCurso(cursoOpt.get());
        aluno.setNumero_casa(dto.getNumero_casa());
        aluno.setComplemento(dto.getComplemento());

        if (dto.getCep() != null && !dto.getCep().trim().isEmpty()) {
            if (dto.getCep().length() != 8) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ResponseModel("O CEP deve conter 8 dígitos."));
            }
            try {
                AlunoDTO enderecoDTO = cepService.buscarEnderecoPorCep(dto.getCep());
                if (enderecoDTO != null && enderecoDTO.getCep() != null) {
                    aluno.setCep(enderecoDTO.getCep().replace("-", ""));
                    aluno.setLogradouro(enderecoDTO.getLogradouro());
                    aluno.setLocalidade(enderecoDTO.getLocalidade());
                    aluno.setBairro(enderecoDTO.getBairro());
                    aluno.setUf(enderecoDTO.getUf());
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new ResponseModel("CEP inválido ou não encontrado."));
                }
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new ResponseModel("Erro ao consultar o serviço de CEP."));
            }
        }

        UsuarioModel usuario = new UsuarioModel();
        usuario.setEmail(dto.getEmail()); // Usando email como login
        usuario.setSenha(passwordEncoder.encode(dto.getMatricula())); // Senha inicial é a matricula
        usuario.setRole(Role.ALUNO);
        usuario.setAluno(aluno);
        aluno.setUsuario(usuario);

        AlunoModel salvo = ar.save(aluno);
        emailService.enviarSenhaInicial(aluno.getEmail(), aluno.getNomeCompleto(), dto.getMatricula());

        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    public ResponseEntity<?> atualizar(String matricula, AlunoDTO dto) {
        Optional<AlunoModel> alunoOpt = ar.findById(matricula);
        if (alunoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResponseModel("Aluno não encontrado para a matrícula: " + matricula));
        }

        AlunoModel aluno = alunoOpt.get();

        if (dto.getNomeCompleto() == null || dto.getNomeCompleto().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseModel("O nome completo é obrigatório."));
        }
        if (dto.getCpf() == null || dto.getCpf().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseModel("O CPF é obrigatório."));
        }
        if (!CpfValidator.isCpfValido(dto.getCpf())) {
            return ResponseEntity.badRequest().body(new ResponseModel("CPF inválido."));
        }

        var curso = cursoRepository.findById(dto.getCursoId());
        if (curso.isEmpty()) {
            return ResponseEntity.badRequest().body(new ResponseModel("Curso não encontrado."));
        }

        AlunoDTO enderecoDTO = cepService.buscarEnderecoPorCep(dto.getCep());
        if (enderecoDTO == null || enderecoDTO.getCep() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ResponseModel("CEP inválido ou não encontrado."));
        }

        boolean cpfAlterado = !aluno.getCpf().equals(dto.getCpf());

        aluno.setNomeCompleto(dto.getNomeCompleto());
        aluno.setCpf(dto.getCpf());
        aluno.setDataNascimento(dto.getDataNascimento());
        aluno.setCelular(dto.getCelular());
        aluno.setEmail(dto.getEmail());
        aluno.setCurso(curso.get());
        aluno.setCep(dto.getCep());
        aluno.setLogradouro(enderecoDTO.getLogradouro());
        aluno.setComplemento(enderecoDTO.getComplemento());
        aluno.setLocalidade(enderecoDTO.getLocalidade());
        aluno.setBairro(enderecoDTO.getBairro());
        aluno.setUf(enderecoDTO.getUf());
        aluno.setNumero_casa(dto.getNumero_casa());

        if (cpfAlterado && aluno.getUsuario() != null) {
            aluno.getUsuario().setSenha(passwordEncoder.encode(dto.getCpf()));
            emailService.enviarSenhaInicial(aluno.getEmail(), aluno.getNomeCompleto(), dto.getCpf());
        }

        AlunoModel salvo = ar.save(aluno);
        return ResponseEntity.ok(salvo);
    }

    public ResponseEntity<ResponseModel> excluir(String matricula) {
        var alunoOpt = ar.findById(matricula);

        if (alunoOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ResponseModel("Aluno não encontrado."));
        }

        AlunoModel aluno = alunoOpt.get();

        if (aluno.getUsuario() != null) {
            ur.delete(aluno.getUsuario());
        }

        ar.delete(aluno);

        return ResponseEntity.ok(new ResponseModel("Aluno e usuário associados removidos com sucesso."));
    }

    public Optional<AlunoModel> buscarPorNome(String nome) {
        return ar.findByNomeCompletoIgnoreCase(nome);
    }

    public Optional<AlunoModel> buscarPorCPF(String cpf) {
        return ar.findByCpf(cpf);
    }

    public List<AlunoModel> buscarTodos() {
        return ar.findAll();
    }
}