package br.com.lumilivre.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.domain.policy.PasswordPolicy;
import br.com.lumilivre.api.dto.auth.ChangePasswordRequest;
import br.com.lumilivre.api.dto.user.UserRequest;
import br.com.lumilivre.api.dto.user.UserStatusRequest;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.repository.AppUserRepository;
import br.com.lumilivre.api.security.Auditable;
import br.com.lumilivre.api.service.infra.EmailService;
import jakarta.persistence.criteria.Predicate;
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

    /**
     * Busca avancada da aba Usuarios. Cada filtro nulo simplesmente nao entra no
     * WHERE: e o que evita o parametro nulo sem tipo que o Postgres nao consegue
     * inferir, e o que permite comparar o papel por igualdade em vez de LIKE
     * sobre enum (que no Postgres virava {@code lower(bytea)} e respondia 500).
     */
    public Page<AppUser> searchUsersAdvanced(UUID id, String email, Role role, Pageable pageable) {
        Specification<AppUser> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (id != null) {
                predicates.add(cb.equal(root.get("id"), id));
            }
            if (email != null && !email.isBlank()) {
                // Igualdade, como no JPQL anterior — a busca por trecho e a do
                // campo de texto livre (buscarPorTexto), nao a do filtro avancado.
                predicates.add(cb.equal(root.get("email"), email.trim()));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return appUserRepository.findAll(spec, pageable);
    }

    // Alvo pelo id, nao pelo e-mail: audit_log nao e lugar de dado de contato.
    @Auditable(action = "USER_CREATED", targetParam = "#result.id")
    @Transactional
    public AppUser createAdmin(UserRequest request) {
        AppUser appUser = AppUser.builder()
                .email(request.getEmail())
                .role(request.getRole())
                .build();
        return saveAdmin(appUser, request.getPassword());
    }

    @Auditable(action = "USER_UPDATED", targetParam = "#id")
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
        // Novo usuário criado por admin deve trocar a senha no 1º acesso.
        appUser.setMustChangePassword(true);

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
            // Senha redefinida pelo admin é conhecida por ele → força troca.
            appUser.setMustChangePassword(true);
            // E as sessões abertas com a senha antiga morrem: sem isso, resetar
            // a senha de uma conta suspeita não expulsava quem já estava dentro.
            appUser.revokeIssuedTokens();
        }

        return appUserRepository.save(appUser);
    }

    /**
     * Liga/desliga o acesso de uma conta (SEC-07). Desativar é desligamento
     * administrativo; bloquear é reação a suspeita de comprometimento.
     *
     * <p>Duas travas impedem tijolar o sistema: ninguém tira o próprio acesso
     * (erraria e perderia o painel) e a última conta ADMIN utilizável não pode
     * cair (não sobraria quem religasse).
     */
    @Auditable(action = "USER_STATUS_CHANGED", targetParam = "#id")
    @Transactional
    public AppUser setStatus(UUID id, UserStatusRequest request) {
        if (request == null || (request.getActive() == null && request.getLocked() == null)) {
            throw BusinessRuleException.ofKey("user.status.nothing-to-change");
        }

        AppUser appUser = appUserRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("user.not-found"));

        boolean newActive = request.getActive() != null
                ? request.getActive()
                : Boolean.TRUE.equals(appUser.getActive());
        boolean newLocked = request.getLocked() != null
                ? request.getLocked()
                : Boolean.TRUE.equals(appUser.getLocked());
        boolean losesAccess = !newActive || newLocked;

        if (losesAccess) {
            if (isCurrentUser(appUser)) {
                throw BusinessRuleException.ofKey("user.status.cannot-disable-self");
            }
            if (isLastUsableAdmin(appUser)) {
                throw BusinessRuleException.ofKey("user.status.cannot-disable-last-admin");
            }
        }

        boolean wasUsable = appUser.canAuthenticate();

        appUser.setActive(newActive);
        appUser.setLocked(newLocked);

        // Perder o acesso tem que valer agora, não quando o token vencer.
        if (wasUsable && losesAccess) {
            appUser.revokeIssuedTokens();
        }

        return appUserRepository.save(appUser);
    }

    private boolean isCurrentUser(AppUser target) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return false;
        }
        String current = authentication.getName();
        if (current.equalsIgnoreCase(target.getEmail())) {
            return true;
        }
        return target.getReader() != null
                && current.equalsIgnoreCase(target.getReader().getRegistrationNumber());
    }

    private boolean isLastUsableAdmin(AppUser target) {
        if (target.getRole() != Role.ADMIN) {
            return false;
        }
        return target.canAuthenticate() && appUserRepository.countUsableAdmins() <= 1;
    }

    @Auditable(action = "USER_DELETED", targetParam = "#id")
    @Transactional
    public void excluir(UUID id) {
        AppUser appUser = appUserRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("user.not-found"));

        // Excluir é a versão irreversível de desativar: as mesmas travas valem,
        // senão o admin some com a própria conta (ou com a última) e ninguém
        // religa o sistema.
        if (isCurrentUser(appUser)) {
            throw BusinessRuleException.ofKey("user.status.cannot-disable-self");
        }
        if (isLastUsableAdmin(appUser)) {
            throw BusinessRuleException.ofKey("user.status.cannot-disable-last-admin");
        }

        if (appUser.getRole() == Role.READER && appUser.getReader() != null) {
            appUser.getReader().setAppUser(null);
        }

        appUserRepository.delete(appUser);
    }

    /** @return a conta já com a senha nova, para o chamador emitir um token válido */
    @Transactional
    public AppUser changePassword(ChangePasswordRequest request) {
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

        PasswordPolicy.validate(
                request.getNewPassword(),
                passwordEncoder.matches(request.getNewPassword(), appUser.getPasswordHash()),
                AuthService.personalDataOf(appUser));

        appUser.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        // Troca concluída → limpa a flag de primeira senha.
        appUser.setMustChangePassword(false);
        // Trocar a senha derruba as sessões abertas com a senha antiga — é o que
        // faltava para um token roubado morrer junto com a senha vazada.
        appUser.revokeIssuedTokens();
        appUserRepository.save(appUser);
        return appUser;
    }

    /** Marca o tour guiado como concluído para o usuário autenticado. Idempotente. */
    @Transactional
    public void completeTour() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = userDetails.getUsername();

        AppUser appUser = appUserRepository.findByEmailOrRegistrationNumber(username, username)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("user.logged-in-not-found"));

        appUser.setGuidedTourCompleted(true);
        appUserRepository.save(appUser);
    }

    /**
     * Idiomas que os clientes de fato usam (pastas de i18n do web/app). E a lista
     * que decide em que lingua saem os e-mails transacionais, entao vive aqui e
     * nao herda a do resolver de request (que aceita variantes soltas como "en"
     * para casar o Accept-Language do navegador).
     */
    private static final java.util.Set<String> SUPPORTED_LOCALES =
            java.util.Set.of("pt-BR", "en-US", "es-ES", "zh-CN", "hi-IN");

    public void updateMyLocale(String locale) {
        if (locale == null || !SUPPORTED_LOCALES.contains(locale.trim())) {
            throw BusinessRuleException.ofKey("user.locale.unsupported");
        }
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = userDetails.getUsername();

        AppUser appUser = appUserRepository.findByEmailOrRegistrationNumber(username, username)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("user.logged-in-not-found"));

        appUser.setPreferredLocale(locale.trim());
        appUserRepository.save(appUser);
    }
}
