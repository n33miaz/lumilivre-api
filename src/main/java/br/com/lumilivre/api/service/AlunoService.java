package br.com.lumilivre.api.service;

import br.com.lumilivre.api.dto.AlunoDTO;
import br.com.lumilivre.api.dto.ListaAlunoDTO;
import br.com.lumilivre.api.enums.Penalidade;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.RecursoNaoEncontradoException;
import br.com.lumilivre.api.exception.custom.RegraDeNegocioException;
import br.com.lumilivre.api.model.*;
import br.com.lumilivre.api.repository.*;
import br.com.lumilivre.api.utils.CpfValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    // MÉTODOS DE BUSCA (READ) - Sem grandes alterações aqui

    public Page<ListaAlunoDTO> buscarAlunosParaListaAdmin(String texto, Pageable pageable) {
        if (texto != null && !texto.isBlank()) {
            return alunoRepository.findAlunosParaListaAdminComFiltro(texto, pageable);
        }
        return alunoRepository.findAlunosParaListaAdmin(pageable);
    }

    public Page<AlunoModel> buscarPorTexto(String texto, Pageable pageable) {
        if (texto == null || texto.isBlank()) {
            return alunoRepository.findAll(pageable);
        }
        return alunoRepository.buscarPorTexto(texto, pageable);
    }

    public Page<AlunoModel> buscarAvancado(String penalidadeStr, String matricula, String nome,
            String cursoNome, Integer turnoId, Integer moduloId, LocalDate dataNascimento,
            String email, String celular, Pageable pageable) {
        Penalidade penalidadeEnum = parseEnum(penalidadeStr, Penalidade.class);
        String nomeFiltro = criarFiltroLike(nome);
        String cursoNomeFiltro = criarFiltroLike(cursoNome);
        String emailFiltro = criarFiltroLike(email);

        return alunoRepository.buscarAvancado(
                penalidadeEnum, matricula, nomeFiltro, cursoNomeFiltro, turnoId, moduloId, dataNascimento,
                emailFiltro, celular, pageable);
    }

    // MÉTODOS DE ESCRITA (CREATE, UPDATE, DELETE)

    @Transactional
    public AlunoModel cadastrar(AlunoDTO dto) {
        validarDadosAluno(dto, false);
        EntidadesRelacionadas entidades = buscarEntidadesRelacionadas(dto);

        AlunoModel aluno = new AlunoModel();
        mapearDtoParaEntidade(aluno, dto, entidades);
        preencherEnderecoPorCep(aluno, dto.getCep());

        UsuarioModel usuario = criarUsuarioParaAluno(aluno);
        aluno.setUsuario(usuario);

        AlunoModel alunoSalvo = alunoRepository.save(aluno);
        emailService.enviarSenhaInicial(aluno.getEmail(), aluno.getNomeCompleto(), dto.getMatricula());

        return alunoSalvo;
    }

    @Transactional
    public AlunoModel atualizar(String matricula, AlunoDTO dto) {
        AlunoModel aluno = alunoRepository.findById(matricula)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Aluno com matrícula " + matricula + " não encontrado."));

        validarDadosAluno(dto, true);
        EntidadesRelacionadas entidades = buscarEntidadesRelacionadas(dto);
        boolean cpfAlterado = !aluno.getCpf().equals(dto.getCpf());

        mapearDtoParaEntidade(aluno, dto, entidades);
        preencherEnderecoPorCep(aluno, dto.getCep());

        if (cpfAlterado && aluno.getUsuario() != null) {
            aluno.getUsuario().setSenha(passwordEncoder.encode(dto.getCpf()));
            emailService.enviarSenhaInicial(aluno.getEmail(), aluno.getNomeCompleto(), dto.getCpf());
        }

        return alunoRepository.save(aluno);
    }

    @Transactional
    public void excluir(String matricula) {
        AlunoModel aluno = alunoRepository.findById(matricula)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Aluno com matrícula " + matricula + " não encontrado."));

        if (aluno.getUsuario() != null) {
            usuarioRepository.delete(aluno.getUsuario());
        }
        alunoRepository.delete(aluno);
    }

    // MÉTODOS PRIVADOS (HELPERS)

    private void validarDadosAluno(AlunoDTO dto, boolean isUpdate) {
        if (!isUpdate) {
            if (dto.getMatricula() == null || dto.getMatricula().isBlank())
                throw new RegraDeNegocioException("A matrícula é obrigatória.");
            if (!dto.getMatricula().matches("\\d{5}"))
                throw new RegraDeNegocioException("A matrícula deve conter 5 dígitos numéricos.");
            if (alunoRepository.existsById(dto.getMatricula()))
                throw new RegraDeNegocioException("Essa matrícula já está cadastrada.");
        }
        if (dto.getNomeCompleto() == null || dto.getNomeCompleto().isBlank())
            throw new RegraDeNegocioException("O nome completo é obrigatório.");
        if (dto.getCpf() == null || dto.getCpf().isBlank())
            throw new RegraDeNegocioException("O CPF é obrigatório.");
        if (!CpfValidator.isCpfValido(dto.getCpf()))
            throw new RegraDeNegocioException("CPF inválido.");
        if (!isUpdate && alunoRepository.existsByCpf(dto.getCpf()))
            throw new RegraDeNegocioException("CPF já cadastrado.");
        if (dto.getEmail() == null || dto.getEmail().isBlank())
            throw new RegraDeNegocioException("O email é obrigatório.");
        if (dto.getCelular() == null || dto.getCelular().isBlank())
            throw new RegraDeNegocioException("O celular é obrigatório.");
        if (dto.getCursoId() == null)
            throw new RegraDeNegocioException("O curso é obrigatório.");
        if (dto.getTurnoId() == null)
            throw new RegraDeNegocioException("O turno é obrigatório.");
        if (dto.getModuloId() == null)
            throw new RegraDeNegocioException("O módulo é obrigatório.");
    }

    private EntidadesRelacionadas buscarEntidadesRelacionadas(AlunoDTO dto) {
        CursoModel curso = cursoRepository.findById(dto.getCursoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Curso com ID " + dto.getCursoId() + " não encontrado."));
        TurnoModel turno = turnoRepository.findById(dto.getTurnoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Turno com ID " + dto.getTurnoId() + " não encontrado."));
        ModuloModel modulo = moduloRepository.findById(dto.getModuloId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Módulo com ID " + dto.getModuloId() + " não encontrado."));
        return new EntidadesRelacionadas(curso, turno, modulo);
    }

    private void mapearDtoParaEntidade(AlunoModel aluno, AlunoDTO dto, EntidadesRelacionadas entidades) {
        if (dto.getMatricula() != null)
            aluno.setMatricula(dto.getMatricula());
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
            if (cep.length() != 8)
                throw new RegraDeNegocioException("O CEP deve conter 8 dígitos.");

            try {
                AlunoDTO enderecoDTO = cepService.buscarEnderecoPorCep(cep);
                if (enderecoDTO != null && enderecoDTO.getCep() != null) {
                    aluno.setCep(enderecoDTO.getCep().replace("-", ""));
                    aluno.setLogradouro(enderecoDTO.getLogradouro());
                    aluno.setLocalidade(enderecoDTO.getLocalidade());
                    aluno.setBairro(enderecoDTO.getBairro());
                    aluno.setUf(enderecoDTO.getUf());
                } else {
                    throw new RegraDeNegocioException("CEP inválido ou não encontrado.");
                }
            } catch (Exception e) {
                throw new RegraDeNegocioException(
                        "Não foi possível validar o CEP. Verifique o número ou tente novamente mais tarde.");
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

    // Métodos de busca simples que retornam Optional ou Listas
    public Optional<AlunoModel> buscarPorNome(String nome) {
        return alunoRepository.findByNomeCompletoIgnoreCase(nome);
    }

    public Optional<AlunoModel> buscarPorCPF(String cpf) {
        return alunoRepository.findByCpf(cpf);
    }

    public List<AlunoModel> buscarTodos() {
        return alunoRepository.findAll();
    }
}