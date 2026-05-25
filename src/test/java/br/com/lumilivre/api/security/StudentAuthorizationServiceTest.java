package br.com.lumilivre.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.BookCopy;
import br.com.lumilivre.api.model.Loan;
import br.com.lumilivre.api.model.Student;
import br.com.lumilivre.api.repository.LoanRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class StudentAuthorizationServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unauthenticatedUsersCannotAccessStudentsOrLoans() {
        StudentAuthorizationService service = service();

        assertThat(service.canAccess("2025001")).isFalse();
        assertThat(service.canAccessLoan(UUID.randomUUID())).isFalse();
    }

    @Test
    void nonCustomPrincipalsAreRejected() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@example.test", "password"));

        assertThat(service().canAccess("2025001")).isFalse();
    }

    @Test
    void adminAndLibrarianCanAccessAnyStudentWithoutLoanLookup() {
        authenticate(Role.ADMIN, null);
        assertThat(service().canAccess("2025001")).isTrue();
        assertThat(service().canAccessLoan(UUID.randomUUID())).isTrue();

        authenticate(Role.LIBRARIAN, null);
        assertThat(service().canAccess("any-registration")).isTrue();
        assertThat(service().canAccessLoan(UUID.randomUUID())).isTrue();

        verify(loanRepository, never()).findById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void studentCanOnlyAccessOwnRegistration() {
        authenticate(Role.STUDENT, student("2025001"));

        assertThat(service().canAccess("2025001")).isTrue();
        assertThat(service().canAccess("2025999")).isFalse();
    }

    @Test
    void studentWithoutLinkedStudentCannotAccessRegistration() {
        authenticate(Role.STUDENT, null);

        assertThat(service().canAccess("2025001")).isFalse();
    }

    @Test
    void studentCanAccessOwnLoanOnly() {
        UUID ownLoanId = UUID.randomUUID();
        UUID otherLoanId = UUID.randomUUID();
        authenticate(Role.STUDENT, student("2025001"));
        when(loanRepository.findById(ownLoanId)).thenReturn(Optional.of(loanFor("2025001")));
        when(loanRepository.findById(otherLoanId)).thenReturn(Optional.of(loanFor("2025999")));

        assertThat(service().canAccessLoan(ownLoanId)).isTrue();
        assertThat(service().canAccessLoan(otherLoanId)).isFalse();
    }

    @Test
    void studentCannotAccessMissingNullOrUnownedLoans() {
        UUID missingLoanId = UUID.randomUUID();
        authenticate(Role.STUDENT, student("2025001"));
        when(loanRepository.findById(missingLoanId)).thenReturn(Optional.empty());

        assertThat(service().canAccessLoan(missingLoanId)).isFalse();
        assertThat(service().canAccessLoan(null)).isFalse();
    }

    private StudentAuthorizationService service() {
        return new StudentAuthorizationService(loanRepository);
    }

    private static void authenticate(Role role, Student student) {
        AppUser appUser = AppUser.builder()
                .email(role.name().toLowerCase() + "@example.test")
                .passwordHash("hash")
                .role(role)
                .student(student)
                .build();
        CustomUserDetails principal = new CustomUserDetails(appUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private static Student student(String registrationNumber) {
        return Student.builder()
                .registrationNumber(registrationNumber)
                .fullName("Ada Lovelace")
                .build();
    }

    private static Loan loanFor(String registrationNumber) {
        return Loan.builder()
                .id(UUID.randomUUID())
                .borrowedAt(OffsetDateTime.now())
                .dueAt(OffsetDateTime.now().plusDays(7))
                .student(student(registrationNumber))
                .bookCopy(BookCopy.builder()
                        .copyCode("T001")
                        .book(Book.builder().id(UUID.randomUUID()).title("Clean Code").build())
                        .build())
                .build();
    }
}
