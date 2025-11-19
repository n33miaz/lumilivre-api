package br.com.lumilivre.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.dto.auth.AlterarSenhaRequest;
import br.com.lumilivre.api.dto.usuario.UsuarioResumoResponse;
import br.com.lumilivre.api.dto.usuario.UsuarioRequest;
import br.com.lumilivre.api.dto.usuario.UsuarioResponse;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.RecursoNaoEncontradoException;
import br.com.lumilivre.api.exception.custom.RegraDeNegocioException;
import br.com.lumilivre.api.model.UsuarioModel;
import br.com.lumilivre.api.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UsuarioRepository ur;

    UsuarioService(EmailService emailService) {
        this.emailService = emailService;
    }

    public Page<UsuarioResumoResponse> buscarUsuarioParaListaAdmin(Pageable pageable) {
        return ur.findUsuarioParaListaAdmin(pageable);
    }

    public Page<UsuarioResumoResponse> buscarPorTexto(String texto, Pageable pageable) {
        return ur.buscarPorTextoComDTO(texto, pageable);
    }

    public Page<UsuarioResumoResponse> buscarAvancado(Integer id, String email, Role role, Pageable pageable) {
        return ur.buscarAvancadoComDTO(id, email, role, pageable);
    }

    @Transactional
    public UsuarioResponse cadastrarAdmin(UsuarioRequest dto) {
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new RegraDeNegocioException("O e-mail é obrigatório");
        }

        if (ur.existsByEmail(dto.getEmail())) {
            throw new RegraDeNegocioException("E-mail já está em uso");
        }

        if (dto.getSenha() == null || dto.getSenha().isBlank()) {
            throw new RegraDeNegocioException("A senha é obrigatória");
        }

        UsuarioModel usuarioModel = new UsuarioModel();
        usuarioModel.setEmail(dto.getEmail());
        usuarioModel.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuarioModel.setRole(Role.ADMIN);

        UsuarioModel salvo = ur.save(usuarioModel);

        try {
            emailService.enviarSenhaInicial(dto.getEmail(), "Admin", dto.getSenha());
        } catch (Exception e) {
            System.err.println("Erro ao enviar email: " + e.getMessage());
        }

        return new UsuarioResponse(salvo);
    }

    @Transactional
    public UsuarioResponse atualizar(Integer id, UsuarioRequest dto) {
        UsuarioModel usuarioModel = ur.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new RegraDeNegocioException("O e-mail é obrigatório");
        }

        if (!dto.getEmail().equals(usuarioModel.getEmail()) && ur.existsByEmail(dto.getEmail())) {
            throw new RegraDeNegocioException("E-mail já está em uso");
        }

        usuarioModel.setEmail(dto.getEmail());

        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            usuarioModel.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        UsuarioModel salvo = ur.save(usuarioModel);

        return new UsuarioResponse(salvo);
    }

    @Transactional
    public void excluir(Integer id) {
        UsuarioModel usuario = ur.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        if (usuario.getRole() == Role.ALUNO && usuario.getAluno() != null) {
            usuario.getAluno().setUsuario(null);
        }

        ur.delete(usuario);
    }

    @Transactional
    public void alterarSenha(AlterarSenhaRequest dto) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String usernameLogado = userDetails.getUsername();

        UsuarioModel usuario = ur.findByEmailOrAluno_Matricula(usernameLogado, usernameLogado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário logado não encontrado no sistema."));

        if (usuario.getRole() == Role.ALUNO) {
            if (usuario.getAluno() == null || !usuario.getAluno().getMatricula().equals(dto.getMatricula())) {
                throw new AccessDeniedException("Você não tem permissão para alterar a senha de outro usuário.");
            }
        }

        if (!passwordEncoder.matches(dto.getSenhaAtual(), usuario.getSenha())) {
            throw new RegraDeNegocioException("Senha atual incorreta");
        }

        usuario.setSenha(passwordEncoder.encode(dto.getNovaSenha()));
        ur.save(usuario);
    }
}