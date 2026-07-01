package br.com.lumilivre.api.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.dto.auth.ChangePasswordRequest;
import br.com.lumilivre.api.dto.user.UserRequest;
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
    private final MessageResolver messages;

    public Page<AppUser> listForAdmin(Pageable pageable) {
        return appUserRepository.findAll(pageable);
    }

    public Page<AppUser> searchUsers(String text, Pageable pageable) {
        return appUserRepository.buscarPorTexto(text, pageable);
    }

    public Page<AppUser> searchUsersAdvanced(UUID id, String email, Role role, Pageable pageable) {
        return appUserRepository.buscarAvancado(id, email, role, pageable);
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
    public AppUser updateUser(UUID id, UserRequest request) {
        return updateUser(id, request.getEmail(), request.getPassword());
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

        if (appUser.getRole() == Role.READER) {
            throw BusinessRuleException.ofKey("user.cannot-register-reader-here");
        }

        AppUser savedAppUser = appUserRepository.save(appUser);

        try {
            Locale locale = localeFor(savedAppUser);
            String roleKey = savedAppUser.getRole() == Role.ADMIN ? "user.role.admin" : "user.role.librarian";
            String roleName = messages.resolve(roleKey, locale);
            emailService.enviarSenhaInicialAdmin(savedAppUser.getEmail(), roleName, rawPassword, locale);
        } catch (Exception e) {
            System.err.println("Erro ao enviar email: " + e.getMessage());
        }

        return savedAppUser;
    }

    private Locale localeFor(AppUser appUser) {
        if (appUser != null && appUser.getPreferredLocale() != null && !appUser.getPreferredLocale().isBlank()) {
            return Locale.forLanguageTag(appUser.getPreferredLocale());
        }
        return Locale.forLanguageTag("pt-BR");
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

        if (appUser.getRole() == Role.READER && appUser.getReader() != null) {
            appUser.getReader().setAppUser(null);
        }

        appUserRepository.delete(appUser);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String usernameLogado = userDetails.getUsername();

        AppUser appUser = appUserRepository.findByEmailOrRegistrationNumber(usernameLogado, usernameLogado)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("user.logged-in-not-found"));

        if (appUser.getRole() == Role.READER
                && (appUser.getReader() == null
                || !appUser.getReader().getRegistrationNumber().equals(request.getRegistrationNumber()))) {
            throw new AccessDeniedException("user.change-password.forbidden");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), appUser.getPasswordHash())) {
            throw BusinessRuleException.ofKey("user.password.incorrect");
        }

        appUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        appUserRepository.save(appUser);
    }
}
