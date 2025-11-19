package br.com.lumilivre.api.service;

import br.com.lumilivre.api.dto.aluno.AlunoRequest;
import br.com.lumilivre.api.dto.aluno.AlunoResumoResponse;
import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.RecursoNaoEncontradoException;
import br.com.lumilivre.api.exception.custom.RegraDeNegocioException;
import br.com.lumilivre.api.model.*;
import br.com.lumilivre.api.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class AlunoService {

    private final AlunoRepository alunoRepository;
    private final CursoRepository cursoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TurnoRepository turnoRepository;
    private final ModuloRepository moduloRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final CepService cepService;

    private record EntidadesRelacionadas(CursoModel curso, TurnoModel turno, ModuloModel modulo) {
    }

    public AlunoService(AlunoRepository alunoRepository, CursoRepository cursoRepository,
            UsuarioRepository usuarioRepository, TurnoRepository turnoRepository,
            ModuloRepository moduloRepository, EmailService emailService,
            PasswordEncoder passwordEncoder, CepService cepService) {
        this.alunoRepository = alunoRepository;
        this.cursoRepository = cursoRepository;
        this.usuarioRepository = usuarioRepository;
        this.turnoRepository = turnoRepository;
        this.moduloRepository = moduloRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.cepService = cepService;
    }

    // --- MÉTODOS DE LEITURA ---

    public Page<AlunoResumoResponse> buscarAlunosParaListaAdmin(String texto, Pageable pageable) {
        if (texto != null && !texto.isBlank()) {
            return alunoRepository.findAlunosParaListaAdminComFiltro(texto, pageable);
        }
        return alunoRepository.findAlunosParaListaAdmin(pageable);
    }

    public Page<AlunoResumoResponse> buscarPorTexto(String texto, Pageable pageable) {
        if (texto == null || texto.isBlank()) {
            return alunoRepository.findAlunosParaListaAdmin(pageable);
        }
        return alunoRepository.findAlunosParaListaAdminComFiltro(texto, pageable);
    }

    public Page<AlunoResumoResponse> buscarAvancado(String penalidadeStr, String matricula, String nome,
            String cursoNome, Integer turnoId, Integer moduloId, LocalDate dataNascimento,
            String email, String celular, Pageable pageable) {
        Penalidade penalidadeEnum = parseEnum(penalidadeStr, Penalidade.class);
        String nomeFiltro = criarFiltroLike(nome);
        String cursoNomeFiltro = criarFiltroLike(cursoNome);
        String emailFiltro = criarFiltroLike(email);

        return alunoRepository.buscarAvancadoComDTO(
                penalidadeEnum, matricula, nomeFiltro, cursoNomeFiltro, turnoId, moduloId, dataNascimento,
                emailFiltro, celular, pageable);
    }

    public AlunoModel buscarPorMatricula(String matricula) {
        return alunoRepository.findByMatricula(matricula)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));
    }

    // --- MÉTODOS DE ESCRITA ---

    @Transactional
    public AlunoModel cadastrar(AlunoRequest dto) {
        if (alunoRepository.existsByMatricula(dto.getMatricula())) {
            throw new RegraDeNegocioException("Matrícula já cadastrada.");
        }
        if (alunoRepository.existsByCpf(dto.getCpf())) {
            throw new RegraDeNegocioException("CPF já cadastrado.");
        }
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new RegraDeNegocioException("E-mail já está em uso.");
        }

        EntidadesRelacionadas entidades = buscarEntidadesRelacionadas(dto);

        AlunoModel aluno = new AlunoModel();
        mapearDtoParaEntidade(aluno, dto, entidades);

        preencherEnderecoPorCep(aluno, dto.getCep());

        UsuarioModel usuario = criarUsuarioParaAluno(aluno);
        aluno.setUsuario(usuario);

        AlunoModel alunoSalvo = alunoRepository.save(aluno);

        // pode ser assíncrono no futuro
        try {
            emailService.enviarSenhaInicial(aluno.getEmail(), aluno.getNomeCompleto(), dto.getMatricula());
        } catch (Exception e) {
            System.err.println("Erro ao enviar email: " + e.getMessage());
        }

        return alunoSalvo;
    }

    @Transactional
    public AlunoModel atualizar(String matricula, AlunoRequest dto) {
        AlunoModel aluno = alunoRepository.findByMatricula(matricula)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));

        if (!aluno.getCpf().equals(dto.getCpf()) && alunoRepository.existsByCpf(dto.getCpf())) {
            throw new RegraDeNegocioException("Este CPF já está sendo usado por outro aluno.");
        }

        EntidadesRelacionadas entidades = buscarEntidadesRelacionadas(dto);

        boolean cpfAlterado = !aluno.getCpf().equals(dto.getCpf());

        mapearDtoParaEntidade(aluno, dto, entidades);
        preencherEnderecoPorCep(aluno, dto.getCep());

        if (cpfAlterado && aluno.getUsuario() != null) {
            aluno.getUsuario().setSenha(passwordEncoder.encode(dto.getCpf()));
        }

        if (aluno.getUsuario() != null && !aluno.getEmail().equals(aluno.getUsuario().getEmail())) {
            aluno.getUsuario().setEmail(aluno.getEmail());
        }

        return alunoRepository.save(aluno);
    }

    @Transactional
    public void excluir(String matricula) {
        AlunoModel aluno = alunoRepository.findByMatricula(matricula)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado."));

        if (aluno.getUsuario() != null) {
            usuarioRepository.delete(aluno.getUsuario());
        }
        alunoRepository.delete(aluno);
    }

    // --- MÉTODOS PRIVADOS ---

    private EntidadesRelacionadas buscarEntidadesRelacionadas(AlunoRequest dto) {
        CursoModel curso = cursoRepository.findById(dto.getCursoId())
                .orElseThrow(
                        () -> new RecursoNaoEncontradoException("Curso não encontrado (ID: " + dto.getCursoId() + ")"));

        TurnoModel turno = turnoRepository.findById(dto.getTurnoId())
                .orElseThrow(
                        () -> new RecursoNaoEncontradoException("Turno não encontrado (ID: " + dto.getTurnoId() + ")"));

        ModuloModel modulo = moduloRepository.findById(dto.getModuloId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Módulo não encontrado (ID: " + dto.getModuloId() + ")"));

        return new EntidadesRelacionadas(curso, turno, modulo);
    }

    private void mapearDtoParaEntidade(AlunoModel aluno, AlunoRequest dto, EntidadesRelacionadas entidades) {
        if (dto.getMatricula() != null) {
            aluno.setMatricula(dto.getMatricula());
        }
        aluno.setNomeCompleto(dto.getNomeCompleto());
        aluno.setCpf(dto.getCpf());
        aluno.setDataNascimento(dto.getDataNascimento());
        aluno.setCelular(dto.getCelular());
        aluno.setEmail(dto.getEmail());

        aluno.setNumero_casa(dto.getNumeroCasa());
        aluno.setComplemento(dto.getComplemento());

        aluno.setCurso(entidades.curso());
        aluno.setTurno(entidades.turno());
        aluno.setModulo(entidades.modulo());
    }

    private void preencherEnderecoPorCep(AlunoModel aluno, String cep) {
        if (cep != null && !cep.isBlank()) {
            String cepLimpo = cep.replace("-", "").trim();
            if (cepLimpo.length() != 8)
                return;

            try {
                AlunoRequest enderecoDTO = cepService.buscarEnderecoPorCep(cepLimpo);
                if (enderecoDTO != null && enderecoDTO.getLogradouro() != null) {
                    aluno.setCep(cepLimpo);
                    aluno.setLogradouro(enderecoDTO.getLogradouro());
                    aluno.setLocalidade(enderecoDTO.getLocalidade());
                    aluno.setBairro(enderecoDTO.getBairro());
                    aluno.setUf(enderecoDTO.getUf());
                }
            } catch (Exception e) {
                // falha silenciosa
            }
        }
    }

    private UsuarioModel criarUsuarioParaAluno(AlunoModel aluno) {
        UsuarioModel usuario = new UsuarioModel();
        usuario.setEmail(aluno.getEmail());
        usuario.setSenha(passwordEncoder.encode(aluno.getMatricula())); // Senha inicial é a matrícula
        usuario.setRole(Role.ALUNO);
        usuario.setAluno(aluno);
        return usuario;
    }

    private <T extends Enum<T>> T parseEnum(String value, Class<T> enumClass) {
        if (value == null || value.isBlank())
            return null;
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