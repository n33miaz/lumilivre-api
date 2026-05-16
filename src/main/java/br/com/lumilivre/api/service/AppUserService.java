package br.com.lumilivre.api.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.dto.auth.AlterarSenhaRequest;
import br.com.lumilivre.api.dto.user.UserRequest;
import br.com.lumilivre.api.dto.v1.usuario.UsuarioRequest;
import br.com.lumilivre.api.dto.v1.usuario.UsuarioResponse;
import br.com.lumilivre.api.dto.v1.usuario.UsuarioResumoResponse;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
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

    public Page<AppUser> listForAdmin(Pageable pageable) {
        return appUserRepository.findAll(pageable);
    }

    public Page<AppUser> searchUsers(String text, Pageable pageable) {
        return appUserRepository.buscarPorTexto(text, pageable);
    }

    public Page<AppUser> searchUsersAdvanced(UUID id, String email, Role role, Pageable pageable) {
        return appUserRepository.buscarAvancado(id, email, role, pageable);
    }

    public Page<UsuarioResumoResponse> buscarUsuarioParaListaAdmin(Pageable pageable) {
        return appUserRepository.findUsuarioParaListaAdmin(pageable);
    }

    public Page<UsuarioResumoResponse> buscarPorTexto(String texto, Pageable pageable) {
        return appUserRepository.buscarPorTextoComDTO(texto, pageable);
    }

    public Page<UsuarioResumoResponse> buscarAvancado(UUID id, String email, Role role, Pageable pageable) {
        return appUserRepository.buscarAvancadoComDTO(id, email, role, pageable);
    }

    @Transactional
    public AppUser createAdmin(UserRequest request) {
        AppUser appUser = AppUser.builder()
                .email(request.getEmail())
                .role(request.getRole())
                .build();
        return saveAdmin(appUser, request.getPassword());
    }

    @Transactional
    public UsuarioResponse cadastrarAdmin(UsuarioRequest dto) {
        AppUser appUser = AppUser.builder()
                .email(dto.getEmail())
                .role(dto.getRole())
                .build();
        return new UsuarioResponse(saveAdmin(appUser, dto.getSenha()));
    }

    @Transactional
    public AppUser updateUser(UUID id, UserRequest request) {
        return updateUser(id, request.getEmail(), request.getPassword());
    }

    @Transactional
    public UsuarioResponse atualizar(UUID id, UsuarioRequest dto) {
        return new UsuarioResponse(updateUser(id, dto.getEmail(), dto.getSenha()));
    }

    private AppUser saveAdmin(AppUser appUser, String rawPassword) {
        if (appUser.getEmail() == null || appUser.getEmail().isBlank()) {
            throw BusinessRuleException.ofKey("user.email.required");
        }
        if (appUserRepository.existsByEmail(appUser.getEmail())) {
            throw BusinessRuleException.ofKey("user.email.in-use");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw BusinessRuleException.ofKey("user.password.required");
        }

        appUser.setPasswordHash(passwordEncoder.encode(rawPassword));
        appUser.setRole(appUser.getRole() != null ? appUser.getRole() : Role.LIBRARIAN);

        if (appUser.getRole() == Role.STUDENT) {
            throw BusinessRuleException.ofKey("user.cannot-register-student-here");
        }

        AppUser savedAppUser = appUserRepository.save(appUser);

        try {
            String roleName = savedAppUser.getRole() == Role.ADMIN ? "Administrador" : "Bibliotecário";
            emailService.enviarSenhaInicialAdmin(savedAppUser.getEmail(), roleName, rawPassword);
        } catch (Exception e) {
            System.err.println("Erro ao enviar email: " + e.getMessage());
        }

        return savedAppUser;
    }

    private AppUser updateUser(UUID id, String email, String rawPassword) {
        AppUser appUser = appUserRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("user.not-found"));

        if (email == null || email.isBlank()) {
            throw BusinessRuleException.ofKey("user.email.required");
        }

        if (!email.equals(appUser.getEmail()) && appUserRepository.existsByEmail(email)) {
            throw BusinessRuleException.ofKey("user.email.in-use");
        }

        appUser.setEmail(email);

        if (rawPassword != null && !rawPassword.isBlank()) {
            appUser.setPasswordHash(passwordEncoder.encode(rawPassword));
        }

        return appUserRepository.save(appUser);
    }

    @Transactional
    public void excluir(UUID id) {
        AppUser appUser = appUserRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("user.not-found"));

        if (appUser.getRole() == Role.STUDENT && appUser.getStudent() != null) {
            appUser.getStudent().setAppUser(null);
        }

        appUserRepository.delete(appUser);
    }

    @Transactional
    public void alterarSenha(AlterarSenhaRequest dto) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String usernameLogado = userDetails.getUsername();

        AppUser appUser = appUserRepository.findByEmailOrRegistrationNumber(usernameLogado, usernameLogado)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("user.logged-in-not-found"));

        if (appUser.getRole() == Role.STUDENT
                && (appUser.getStudent() == null
                || !appUser.getStudent().getRegistrationNumber().equals(dto.getMatricula()))) {
            throw new AccessDeniedException("Você não tem permissão para alterar a senha de outro usuário.");
        }

        if (!passwordEncoder.matches(dto.getSenhaAtual(), appUser.getPasswordHash())) {
            throw BusinessRuleException.ofKey("user.password.incorrect");
        }

        appUser.setPasswordHash(passwordEncoder.encode(dto.getNovaSenha()));
        appUserRepository.save(appUser);
    }
}
