package br.com.lumilivre.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.repository.EmprestimoRepository;
import lombok.RequiredArgsConstructor;

@Service("studentAuthz")
@RequiredArgsConstructor
public class StudentAuthorizationService {

    private final EmprestimoRepository emprestimoRepository;

    /**
     * Retorna true se o principal autenticado é ADMIN/BIBLIOTECARIO,
     * ou se é o próprio aluno dono da matrícula informada.
     */
    public boolean canAccess(String matricula) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof CustomUserDetails details)) {
            return false;
        }

        Role role = details.getUsuario().getRole();
        if (role == Role.ADMIN || role == Role.BIBLIOTECARIO) {
            return true;
        }

        // Aluno só acessa o próprio recurso
        var alunoVinculado = details.getUsuario().getAluno();
        return alunoVinculado != null && alunoVinculado.getMatricula().equals(matricula);
    }

    /**
     * Retorna true se o principal autenticado e ADMIN/BIBLIOTECARIO,
     * ou se o emprestimo pertence ao aluno autenticado.
     */
    public boolean canAccessLoan(Integer emprestimoId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof CustomUserDetails details)) {
            return false;
        }

        Role role = details.getUsuario().getRole();
        if (role == Role.ADMIN || role == Role.BIBLIOTECARIO) {
            return true;
        }

        var alunoVinculado = details.getUsuario().getAluno();
        if (alunoVinculado == null || emprestimoId == null) {
            return false;
        }

        return emprestimoRepository.findById(emprestimoId)
                .map(emprestimo -> emprestimo.getAluno() != null
                        && alunoVinculado.getMatricula().equals(emprestimo.getAluno().getMatricula()))
                .orElse(false);
    }
}
