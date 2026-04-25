package br.com.lumilivre.api.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.repository.LoanRepository;
import lombok.RequiredArgsConstructor;

@Service("studentAuthz")
@RequiredArgsConstructor
public class StudentAuthorizationService {

    private final LoanRepository loanRepository;

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

        var student = details.getAppUser().getStudent();
        return student != null && student.getRegistrationNumber().equals(matricula);
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

        var student = details.getAppUser().getStudent();
        if (student == null || loanId == null) {
            return false;
        }

        return loanRepository.findById(loanId)
                .map(loan -> loan.getStudent() != null
                        && student.getRegistrationNumber().equals(loan.getStudent().getRegistrationNumber()))
                .orElse(false);
    }
}
