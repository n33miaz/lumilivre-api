package br.com.lumilivre.api.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.dto.auth.AlterarSenhaRequest;
import br.com.lumilivre.api.dto.usuario.UsuarioRequest;
import br.com.lumilivre.api.dto.usuario.UsuarioResponse;
import br.com.lumilivre.api.dto.usuario.UsuarioResumoResponse;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.RecursoNaoEncontradoException;
import br.com.lumilivre.api.exception.custom.RegraDeNegocioException;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.repository.AppUserRepository;
import br.com.lumilivre.api.service.infra.EmailService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AppUserRepository appUserRepository;

    public Page<UsuarioResumoResponse> buscarUsuarioParaListaAdmin(Pageable pageable) {
        return appUserRepository.findUsuarioParaListaAdmin(pageable);
    }

    public Page<UsuarioResumoResponse> buscarPorTexto(String texto, Pageable pageable) {
        return appUserRepository.buscarPorTextoComDTO(texto, pageable);
    }

    public Page<UsuarioResumoResponse> buscarAvancado(Integer id, String email, Role role, Pageable pageable) {
        return appUserRepository.buscarAvancadoComDTO(id, email, role, pageable);
    }

    @Transactional
    public UsuarioResponse cadastrarAdmin(UsuarioRequest dto) {
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new RegraDeNegocioException("O e-mail Ã© obrigatÃ³rio");
        }
        if (appUserRepository.existsByEmail(dto.getEmail())) {
            throw new RegraDeNegocioException("E-mail jÃ¡ estÃ¡ em uso");
        }
        if (dto.getSenha() == null || dto.getSenha().isBlank()) {
            throw new RegraDeNegocioException("A senha Ã© obrigatÃ³ria");
        }

        AppUser appUser = new AppUser();
        appUser.setEmail(dto.getEmail());
        appUser.setSenha(passwordEncoder.encode(dto.getSenha()));
        appUser.setRole(dto.getRole() != null ? dto.getRole() : Role.LIBRARIAN);

        if (dto.getRole() == Role.STUDENT) {
            throw new RegraDeNegocioException("Para cadastrar alunos, use a rota de Alunos.");
        }

        AppUser savedAppUser = appUserRepository.save(appUser);

        try {
            String nomeRole = (savedAppUser.getRole() == Role.ADMIN) ? "Administrador" : "BibliotecÃ¡rio";
            emailService.enviarSenhaInicialAdmin(dto.getEmail(), nomeRole, dto.getSenha());
        } catch (Exception e) {
            System.err.println("Erro ao enviar email: " + e.getMessage());
        }

        return new UsuarioResponse(savedAppUser);
    }

    @Transactional
    public UsuarioResponse atualizar(Integer id, UsuarioRequest dto) {
        AppUser appUser = appUserRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("UsuÃ¡rio nÃ£o encontrado."));

        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new RegraDeNegocioException("O e-mail Ã© obrigatÃ³rio");
        }

        if (!dto.getEmail().equals(appUser.getEmail()) && appUserRepository.existsByEmail(dto.getEmail())) {
            throw new RegraDeNegocioException("E-mail jÃ¡ estÃ¡ em uso");
        }

        appUser.setEmail(dto.getEmail());

        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            appUser.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        AppUser savedAppUser = appUserRepository.save(appUser);
        return new UsuarioResponse(savedAppUser);
    }

    @Transactional
    public void excluir(Integer id) {
        AppUser appUser = appUserRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("UsuÃ¡rio nÃ£o encontrado."));

        if (appUser.getRole() == Role.STUDENT && appUser.getAluno() != null) {
            appUser.getAluno().setUsuario(null);
        }

        appUserRepository.delete(appUser);
    }

    @Transactional
    public void alterarSenha(AlterarSenhaRequest dto) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String usernameLogado = userDetails.getUsername();

        AppUser appUser = appUserRepository.findByEmailOrAluno_Matricula(usernameLogado, usernameLogado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("UsuÃ¡rio logado nÃ£o encontrado no sistema."));

        if (appUser.getRole() == Role.STUDENT
                && (appUser.getAluno() == null || !appUser.getAluno().getMatricula().equals(dto.getMatricula()))) {
            throw new AccessDeniedException("VocÃª nÃ£o tem permissÃ£o para alterar a senha de outro usuÃ¡rio.");
        }

        if (!passwordEncoder.matches(dto.getSenhaAtual(), appUser.getSenha())) {
            throw new RegraDeNegocioException("Senha atual incorreta");
        }

        appUser.setSenha(passwordEncoder.encode(dto.getNovaSenha()));
        appUserRepository.save(appUser);
    }
}
