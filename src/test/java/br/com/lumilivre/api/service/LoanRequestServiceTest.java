package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.domain.policy.BookAvailabilityPolicy.BookAvailabilityViolationException;
import br.com.lumilivre.api.domain.policy.RequestApprovalPolicy.RequestApprovalViolationException;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.LoanRequestStatus;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.BookCopy;
import br.com.lumilivre.api.model.LoanRequest;
import br.com.lumilivre.api.model.OutboxEvent.EventType;
import br.com.lumilivre.api.model.Student;
import br.com.lumilivre.api.repository.BookCopyRepository;
import br.com.lumilivre.api.repository.LoanRepository;
import br.com.lumilivre.api.repository.LoanRequestRepository;
import br.com.lumilivre.api.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class LoanRequestServiceTest {

    private static final UUID BOOK_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID REQUEST_ID = UUID.fromString("00000000-0000-0000-0000-000000000007");

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private LoanRequestRepository loanRequestRepository;

    @Mock
    private LoanService loanService;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private OutboxPublisherService outboxPublisher;

    @Mock
    private MessageResolver messages;

    @InjectMocks
    private LoanRequestService service;

    @BeforeEach
    void stubMessageResolver() {
        lenient().when(messages.resolve(anyString(), any(Locale.class)))
                .thenAnswer(this::resolveMessage);
        lenient().when(messages.resolve(anyString(), any(Locale.class), any()))
                .thenAnswer(this::resolveMessage);
    }

    private String resolveMessage(InvocationOnMock invocation) {
        String key = invocation.getArgument(0);
        boolean isSubject = key.endsWith(".subject");
        Object[] args = invocation.getArguments();
        String title = args.length >= 3 && args[2] != null ? String.valueOf(args[2]) : "";
        if (key.contains("received")) {
            return isSubject ? "Solicitação recebida"
                    : "Sua solicitação do livro '" + title + "' foi recebida.";
        }
        if (key.contains("accepted")) {
            return isSubject ? "Solicitação aceita"
                    : "Sua solicitação do livro '" + title + "' foi aceita.";
        }
        if (key.contains("rejected")) {
            return isSubject ? "Solicitação rejeitada"
                    : "Sua solicitação do livro '" + title + "' foi rejeitada.";
        }
        return key;
    }

    @Test
    void solicitarEmprestimoReturnsBadRequestWhenStudentDoesNotExist() {
        when(studentRepository.findByRegistrationNumber("12345")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.solicitarEmprestimo("12345", "T001"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("student.not-found");

        verify(loanRequestRepository, never()).save(any());
        verify(outboxPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    @Test
    void solicitarEmprestimoReturnsBadRequestWhenBookCopyDoesNotExist() {
        when(studentRepository.findByRegistrationNumber("12345")).thenReturn(Optional.of(student()));
        when(bookCopyRepository.findByCopyCode("T001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.solicitarEmprestimo("12345", "T001"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("book.copy.not-found");

        verify(loanRequestRepository, never()).save(any());
        verify(outboxPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    @Test
    void solicitarEmprestimoCreatesPendingRequestAndPublishesOutbox() {
        when(studentRepository.findByRegistrationNumber("12345")).thenReturn(Optional.of(student()));
        when(bookCopyRepository.findByCopyCode("T001")).thenReturn(Optional.of(bookCopy(BookCopyStatus.AVAILABLE)));
        when(loanRepository.countByStudent_RegistrationNumberAndStatus("12345", LoanStatus.ACTIVE))
                .thenReturn(1L);
        when(loanRepository.countByStudent_RegistrationNumberAndStatus("12345", LoanStatus.OVERDUE))
                .thenReturn(0L);

        var result = service.solicitarEmprestimo("12345", "T001");

        assertThat(result).isEqualTo("request.created");

        ArgumentCaptor<LoanRequest> captor = ArgumentCaptor.forClass(LoanRequest.class);
        verify(loanRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getStudent().getRegistrationNumber()).isEqualTo("12345");
        assertThat(captor.getValue().getBookCopy().getCopyCode()).isEqualTo("T001");
        assertThat(captor.getValue().getStatus()).isEqualTo(LoanRequestStatus.PENDING);

        verify(outboxPublisher).publish(
                eq(EventType.REQUEST_ACCEPTED),
                eq("aluno@lumilivre.test"),
                org.mockito.ArgumentMatchers.contains("recebida"),
                org.mockito.ArgumentMatchers.contains("Livro Teste"),
                any());
    }

    @Test
    void solicitarEmprestimoPorLivroUsesFirstAvailableCopyAndMarksMobileOrigin() {
        when(studentRepository.findByRegistrationNumber("12345")).thenReturn(Optional.of(student()));
        when(bookCopyRepository.findFirstAvailable(BOOK_ID))
                .thenReturn(Optional.of(bookCopy(BookCopyStatus.AVAILABLE)));
        when(loanRepository.countByStudent_RegistrationNumberAndStatus("12345", LoanStatus.ACTIVE))
                .thenReturn(0L);
        when(loanRepository.countByStudent_RegistrationNumberAndStatus("12345", LoanStatus.OVERDUE))
                .thenReturn(0L);

        var result = service.solicitarEmprestimoPorLivro("12345", BOOK_ID);

        assertThat(result).isEqualTo("request.created");

        ArgumentCaptor<LoanRequest> captor = ArgumentCaptor.forClass(LoanRequest.class);
        verify(loanRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getNote()).isEqualTo("Requested via mobile");
        assertThat(captor.getValue().getBookCopy().getCopyCode()).isEqualTo("T001");
    }

    @Test
    void solicitarEmprestimoRejectsUnavailableBookCopy() {
        when(studentRepository.findByRegistrationNumber("12345")).thenReturn(Optional.of(student()));
        when(bookCopyRepository.findByCopyCode("T001")).thenReturn(Optional.of(bookCopy(BookCopyStatus.BORROWED)));
        when(loanRepository.countByStudent_RegistrationNumberAndStatus("12345", LoanStatus.ACTIVE))
                .thenReturn(0L);
        when(loanRepository.countByStudent_RegistrationNumberAndStatus("12345", LoanStatus.OVERDUE))
                .thenReturn(0L);

        assertThatThrownBy(() -> service.solicitarEmprestimo("12345", "T001"))
                .isInstanceOf(BookAvailabilityViolationException.class)
                .hasMessage("book.copy.not-available");

        verify(loanRequestRepository, never()).save(any());
        verify(outboxPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    @Test
    void processarSolicitacaoAceitaCreatesLoanUpdatesStatusAndPublishesOutbox() {
        LoanRequest request = loanRequest(LoanRequestStatus.PENDING);
        when(loanRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

        var result = service.processarSolicitacao(REQUEST_ID, true);

        assertThat(result).isEqualTo("request.processed");
        assertThat(request.getStatus()).isEqualTo(LoanRequestStatus.ACCEPTED);

        ArgumentCaptor<br.com.lumilivre.api.dto.loan.LoanRequest> requestCaptor =
                ArgumentCaptor.forClass(br.com.lumilivre.api.dto.loan.LoanRequest.class);
        verify(loanService).cadastrar(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getStudentRegistrationNumber()).isEqualTo("12345");
        assertThat(requestCaptor.getValue().getCopyCode()).isEqualTo("T001");
        assertThat(requestCaptor.getValue().getDueAt()).isAfter(requestCaptor.getValue().getBorrowedAt());

        verify(loanRequestRepository).save(request);
        verify(outboxPublisher).publish(
                eq(EventType.REQUEST_ACCEPTED),
                eq("aluno@lumilivre.test"),
                org.mockito.ArgumentMatchers.contains("aceita"),
                org.mockito.ArgumentMatchers.contains("aceita"),
                any());
    }

    @Test
    void processarSolicitacaoRejeitadaUpdatesStatusWithoutCreatingLoan() {
        LoanRequest request = loanRequest(LoanRequestStatus.PENDING);
        when(loanRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

        var result = service.processarSolicitacao(REQUEST_ID, false);

        assertThat(result).isEqualTo("request.processed");
        assertThat(request.getStatus()).isEqualTo(LoanRequestStatus.REJECTED);
        verify(loanService, never()).cadastrar(any(br.com.lumilivre.api.dto.loan.LoanRequest.class));
        verify(loanRequestRepository).save(request);
        verify(outboxPublisher).publish(
                eq(EventType.REQUEST_REJECTED),
                eq("aluno@lumilivre.test"),
                org.mockito.ArgumentMatchers.contains("rejeitada"),
                org.mockito.ArgumentMatchers.contains("rejeitada"),
                any());
    }

    @Test
    void processarSolicitacaoReturnsBadRequestWhenRequestDoesNotExist() {
        when(loanRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processarSolicitacao(REQUEST_ID, true))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("request.not-found");

        verify(loanService, never()).cadastrar(any(br.com.lumilivre.api.dto.loan.LoanRequest.class));
        verify(loanRequestRepository, never()).save(any());
        verify(outboxPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    @Test
    void processarSolicitacaoRejectsAlreadyProcessedRequest() {
        LoanRequest request = loanRequest(LoanRequestStatus.ACCEPTED);
        when(loanRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.processarSolicitacao(REQUEST_ID, false))
                .isInstanceOf(RequestApprovalViolationException.class)
                .hasMessage("request.not-pending");

        verify(loanService, never()).cadastrar(any(br.com.lumilivre.api.dto.loan.LoanRequest.class));
        verify(loanRequestRepository, never()).save(any());
        verify(outboxPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    private static LoanRequest loanRequest(LoanRequestStatus status) {
        LoanRequest request = new LoanRequest();
        request.setId(REQUEST_ID);
        request.setStudent(student());
        request.setBookCopy(bookCopy(BookCopyStatus.AVAILABLE));
        request.setStatus(status);
        return request;
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
        book.setId(BOOK_ID);
        book.setTitle("Livro Teste");

        BookCopy copy = new BookCopy();
        copy.setCopyCode("T001");
        copy.setStatus(status);
        copy.setBook(book);
        return copy;
    }
}
