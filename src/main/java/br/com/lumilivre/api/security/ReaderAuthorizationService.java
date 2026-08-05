package br.com.lumilivre.api.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.repository.LoanRepository;
import br.com.lumilivre.api.service.AccessLogService;
import lombok.RequiredArgsConstructor;

/**
 * Decide se o principal corrente pode alcançar dados de um leitor específico
 * ({@code @CanAccessReader}) ou um empréstimo específico ({@code @CanAccessLoan}).
 *
 * <p>Toda recusa vira {@code ACCESS_DENIED} na trilha de acessos, com o recurso
 * pedido no alvo. Registrar aqui, e não num handler de 403, é deliberado por dois
 * motivos: o {@code GlobalExceptionHandler} intercepta a
 * {@code AccessDeniedException} de segurança de método antes do
 * {@code ExceptionTranslationFilter}, então o handler do {@code SecurityConfig}
 * nunca via essas recusas — as tentativas de IDOR, que são justamente as que uma
 * revisão de segurança procura, não apareciam em lugar nenhum; e aqui existe algo
 * que o handler não tem, que é <b>qual</b> recurso a pessoa tentou abrir.
 *
 * <p>Sem deduplicação, ao contrário dos eventos de uso: numa recusa a repetição
 * <i>é</i> o sinal — uma matrícula varrendo dezenas de outras em sequência é o
 * que se quer ver.
 */
@Service("readerAuthz")
@RequiredArgsConstructor
public class ReaderAuthorizationService {

    private final LoanRepository loanRepository;
    private final AccessLogService accessLogService;

    public boolean canAccess(String matricula) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof CustomUserDetails details)) {
            return false;
        }

        Role role = details.getAppUser().getRole();
        if (role == Role.ADMIN || role == Role.LIBRARIAN) {
            return true;
        }

        var reader = details.getAppUser().getReader();
        if (reader != null && reader.getRegistrationNumber().equals(matricula)) {
            return true;
        }
        recordDenied(auth, matricula, "reader-mismatch");
        return false;
    }

    public boolean canAccessLoan(UUID loanId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof CustomUserDetails details)) {
            return false;
        }

        Role role = details.getAppUser().getRole();
        if (role == Role.ADMIN || role == Role.LIBRARIAN) {
            return true;
        }

        var reader = details.getAppUser().getReader();
        if (reader == null || loanId == null) {
            recordDenied(auth, loanId != null ? loanId.toString() : null, "loan-no-reader");
            return false;
        }

        boolean owns = loanRepository.findById(loanId)
                .map(loan -> loan.getReader() != null
                        && reader.getRegistrationNumber().equals(loan.getReader().getRegistrationNumber()))
                .orElse(false);
        if (!owns) {
            recordDenied(auth, loanId.toString(), "loan-not-owned");
        }
        return owns;
    }

    /**
     * Só grava quando há alguém identificado: recusa de chamador anônimo é
     * barreira de URL, e dar escrita no banco ao anônimo é o oposto do que se
     * quer num endpoint público.
     */
    private void recordDenied(Authentication auth, String target, String reason) {
        String actor = AccessLogService.actorOf(auth);
        if (actor == null) {
            return;
        }
        accessLogService.recordDenied(actor, AccessLogService.roleOf(auth), target, reason);
    }
}
