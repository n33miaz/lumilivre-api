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
import br.com.lumilivre.api.dto.usuario.UsuarioRequest;
import br.com.lumilivre.api.dto.usuario.UsuarioResponse;
import br.com.lumilivre.api.dto.usuario.UsuarioResumoResponse;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
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

    public Page<UsuarioResumoResponse> buscarAvancado(UUID id, String email, Role role, Pageable pageable) {
        return appUserRepository.buscarAvancadoComDTO(id, email, role, pageable);
    }

    @Transactional
    public UsuarioResponse cadastrarAdmin(UsuarioRequest dto) {
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new BusinessRuleException("O e-mail é obrigatório");
        }
        if (appUserRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessRuleException("E-mail já está em uso");
        }
        if (dto.getSenha() == null || dto.getSenha().isBlank()) {
            throw new BusinessRuleException("A senha é obrigatória");
        }

        AppUser appUser = new AppUser();
        appUser.setEmail(dto.getEmail());
        appUser.setPasswordHash(passwordEncoder.encode(dto.getSenha()));
        appUser.setRole(dto.getRole() != null ? dto.getRole() : Role.LIBRARIAN);

        if (dto.getRole() == Role.STUDENT) {
            throw new BusinessRuleException("Para cadastrar alunos, use a rota de Alunos.");
        }

        AppUser savedAppUser = appUserRepository.save(appUser);

        try {
            String nomeRole = (savedAppUser.getRole() == Role.ADMIN) ? "Administrador" : "Bibliotecário";
            emailService.enviarSenhaInicialAdmin(dto.getEmail(), nomeRole, dto.getSenha());
        } catch (Exception e) {
            System.err.println("Erro ao enviar email: " + e.getMessage());
        }

        return new UsuarioResponse(savedAppUser);
    }

    @Transactional
    public UsuarioResponse atualizar(UUID id, UsuarioRequest dto) {
        AppUser appUser = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new BusinessRuleException("O e-mail é obrigatório");
        }

        if (!dto.getEmail().equals(appUser.getEmail()) && appUserRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessRuleException("E-mail já está em uso");
        }

        appUser.setEmail(dto.getEmail());

        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            appUser.setPasswordHash(passwordEncoder.encode(dto.getSenha()));
        }

        AppUser savedAppUser = appUserRepository.save(appUser);
        return new UsuarioResponse(savedAppUser);
    }

    @Transactional
    public void excluir(UUID id) {
        AppUser appUser = appUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

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
                .orElseThrow(() -> new ResourceNotFoundException("Usuário logado não encontrado no sistema."));

        if (appUser.getRole() == Role.STUDENT
                && (appUser.getStudent() == null || !appUser.getStudent().getRegistrationNumber().equals(dto.getMatricula()))) {
            throw new AccessDeniedException("Você não tem permissão para alterar a senha de outro usuário.");
        }

        if (!passwordEncoder.matches(dto.getSenhaAtual(), appUser.getPasswordHash())) {
            throw new BusinessRuleException("Senha atual incorreta");
        }

        appUser.setPasswordHash(passwordEncoder.encode(dto.getNovaSenha()));
        appUserRepository.save(appUser);
    }
}
