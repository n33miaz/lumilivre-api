package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.lumilivre.api.domain.policy.BookAvailabilityPolicy.BookAvailabilityViolationException;
import br.com.lumilivre.api.domain.policy.LoanPolicy.LoanPolicyViolationException;
import br.com.lumilivre.api.dto.v1.emprestimo.EmprestimoRequest;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.BookCopy;
import br.com.lumilivre.api.model.Loan;
import br.com.lumilivre.api.model.Student;
import br.com.lumilivre.api.repository.BookCopyRepository;
import br.com.lumilivre.api.repository.LoanRepository;
import br.com.lumilivre.api.repository.ReservationRepository;
import br.com.lumilivre.api.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private OutboxPublisherService outboxPublisher;

    @InjectMocks
    private LoanService service;

    @Test
    void cadastrarRejectsDueDateBeforeBorrowDate() {
        EmprestimoRequest request = request();
        request.setData_devolucao(request.getData_emprestimo().minusDays(1));

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("devolu");

        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void cadastrarRejectsStudentAtActiveLoanLimit() {
        when(studentRepository.findByRegistrationNumber("12345")).thenReturn(Optional.of(student()));
        when(loanRepository.countByStudent_RegistrationNumberAndStatus("12345", LoanStatus.ACTIVE))
                .thenReturn((long) LoanStatus.values().length);
        when(loanRepository.countByStudent_RegistrationNumberAndStatus("12345", LoanStatus.OVERDUE))
                .thenReturn(0L);

        assertThatThrownBy(() -> service.cadastrar(request()))
                .isInstanceOf(LoanPolicyViolationException.class);

        verify(bookCopyRepository, never()).findByCopyCode(any());
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void cadastrarRejectsStudentWithActivePenalty() {
        Student student = student();
        student.setPenaltyExpiresAt(OffsetDateTime.now().plusDays(2));
        when(studentRepository.findByRegistrationNumber("12345")).thenReturn(Optional.of(student));
        when(loanRepository.countByStudent_RegistrationNumberAndStatus(any(), eq(LoanStatus.ACTIVE))).thenReturn(0L);
        when(loanRepository.countByStudent_RegistrationNumberAndStatus(any(), eq(LoanStatus.OVERDUE))).thenReturn(0L);

        assertThatThrownBy(() -> service.cadastrar(request()))
                .isInstanceOf(LoanPolicyViolationException.class);

        verify(bookCopyRepository, never()).findByCopyCode(any());
        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void cadastrarRejectsUnavailableBookCopy() {
        when(studentRepository.findByRegistrationNumber("12345")).thenReturn(Optional.of(student()));
        when(loanRepository.countByStudent_RegistrationNumberAndStatus(any(), eq(LoanStatus.ACTIVE))).thenReturn(0L);
        when(loanRepository.countByStudent_RegistrationNumberAndStatus(any(), eq(LoanStatus.OVERDUE))).thenReturn(0L);
        when(bookCopyRepository.findByCopyCode("T001")).thenReturn(Optional.of(bookCopy(BookCopyStatus.BORROWED)));

        assertThatThrownBy(() -> service.cadastrar(request()))
                .isInstanceOf(BookAvailabilityViolationException.class);

        verify(loanRepository, never()).save(any(Loan.class));
    }

    private static EmprestimoRequest request() {
        OffsetDateTime now = OffsetDateTime.now();
        return EmprestimoRequest.builder()
                .aluno_matricula("12345")
                .exemplar_tombo("T001")
                .data_emprestimo(now)
                .data_devolucao(now.plusDays(14))
                .build();
    }

    private static Student student() {
        Student student = new Student();
        student.setRegistrationNumber("12345");
        student.setFullName("Aluno Teste");
        student.setEmail("aluno@lumilivre.test");
        return student;
    }

    private static BookCopy bookCopy(BookCopyStatus status) {
        Book book = new Book();
        book.setId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        book.setTitle("Livro Teste");

        BookCopy copy = new BookCopy();
        copy.setCopyCode("T001");
        copy.setStatus(status);
        copy.setBook(book);
        return copy;
    }
}
