package br.com.lumilivre.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.enums.Role;

@Service("studentAuthz")
public class StudentAuthorizationService {

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
}
