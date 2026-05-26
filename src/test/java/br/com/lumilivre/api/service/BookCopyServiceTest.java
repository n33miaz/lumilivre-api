package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.lumilivre.api.dto.book.BookCopyRequest;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.BookCopy;
import br.com.lumilivre.api.repository.BookCopyRepository;
import br.com.lumilivre.api.repository.BookRepository;
import br.com.lumilivre.api.repository.LoanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookCopyServiceTest {

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LoanRepository loanRepository;

    @Captor
    private ArgumentCaptor<BookCopy> bookCopyCaptor;

    @Test
    void listReturnsAllCopiesFromRepository() {
        BookCopy first = BookCopy.builder().copyCode("T001").build();
        BookCopy second = BookCopy.builder().copyCode("T002").build();
        when(bookCopyRepository.findAll()).thenReturn(List.of(first, second));

        assertThat(service().buscarTodos()).containsExactly(first, second);
    }

    @Test
    void listByBookRejectsNullBookId() {
        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().buscarExemplaresPorLivroId(null))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("book.copy.book-id.required"));
        verify(bookRepository, never()).existsById(any());
    }

    @Test
    void listByBookRejectsMissingBook() {
        UUID bookId = UUID.randomUUID();
        when(bookRepository.existsById(bookId)).thenReturn(false);

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service().buscarExemplaresPorLivroId(bookId))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("book.not-found-by-provided-id"));
        verify(bookCopyRepository, never()).findAllByBookIdWithDetails(any());
    }

    @Test
    void listByBookReturnsCopiesWithDetails() {
        UUID bookId = UUID.randomUUID();
        BookCopy copy = BookCopy.builder().copyCode("T001").build();
        when(bookRepository.existsById(bookId)).thenReturn(true);
        when(bookCopyRepository.findAllByBookIdWithDetails(bookId)).thenReturn(List.of(copy));

        assertThat(service().buscarExemplaresPorLivroId(bookId)).containsExactly(copy);
    }

    @Test
    void createSavesCopyWithParsedStatusAndLinkedBook() {
        UUID bookId = UUID.randomUUID();
        Book book = Book.builder().id(bookId).title("Clean Code").build();
        when(bookCopyRepository.existsByCopyCode("T001")).thenReturn(false);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        service().cadastrar(request(bookId, "T001", "available", "A-01"));

        verify(bookCopyRepository).save(bookCopyCaptor.capture());
        BookCopy saved = bookCopyCaptor.getValue();
        assertThat(saved.getCopyCode()).isEqualTo("T001");
        assertThat(saved.getStatus()).isEqualTo(BookCopyStatus.AVAILABLE);
        assertThat(saved.getBook()).isSameAs(book);
        assertThat(saved.getShelfLocation()).isEqualTo("A-01");
    }

    @Test
    void createAcceptsPtBrStatusCode() {
        UUID bookId = UUID.randomUUID();
        Book book = Book.builder().id(bookId).title("Clean Code").build();
        when(bookCopyRepository.existsByCopyCode("T001")).thenReturn(false);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        service().cadastrar(request(bookId, "T001", "DISPONIVEL", "A-01"));

        verify(bookCopyRepository).save(bookCopyCaptor.capture());
        assertThat(bookCopyCaptor.getValue().getStatus()).isEqualTo(BookCopyStatus.AVAILABLE);
    }

    @Test
    void createRejectsDuplicateCopyCodeBeforeLookingUpBook() {
        UUID bookId = UUID.randomUUID();
        when(bookCopyRepository.existsByCopyCode("T001")).thenReturn(true);

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().cadastrar(request(bookId, "T001", "AVAILABLE", "A-01")))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("book.copy.code.already-exists"));
        verify(bookRepository, never()).findById(any());
        verify(bookCopyRepository, never()).save(any());
    }

    @Test
    void createRejectsInvalidStatus() {
        UUID bookId = UUID.randomUUID();
        when(bookCopyRepository.existsByCopyCode("T001")).thenReturn(false);
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(Book.builder().id(bookId).build()));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().cadastrar(request(bookId, "T001", "reserved", "A-01")))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("book.copy.status.invalid"));
        verify(bookCopyRepository, never()).save(any());
    }

    @Test
    void updateChangesBookStatusAndLocation() {
        UUID oldBookId = UUID.randomUUID();
        UUID newBookId = UUID.randomUUID();
        Book oldBook = Book.builder().id(oldBookId).title("Old").build();
        Book newBook = Book.builder().id(newBookId).title("New").build();
        BookCopy copy = BookCopy.builder()
                .copyCode("T001")
                .book(oldBook)
                .status(BookCopyStatus.AVAILABLE)
                .shelfLocation("A-01")
                .build();
        when(bookCopyRepository.findByCopyCode("T001")).thenReturn(Optional.of(copy));
        when(bookRepository.findById(newBookId)).thenReturn(Optional.of(newBook));

        service().atualizar("T001", request(newBookId, "ignored", "maintenance", "B-02"));

        verify(bookCopyRepository).save(bookCopyCaptor.capture());
        BookCopy saved = bookCopyCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(BookCopyStatus.MAINTENANCE);
        assertThat(saved.getBook()).isSameAs(newBook);
        assertThat(saved.getShelfLocation()).isEqualTo("B-02");
    }

    @Test
    void updateRejectsMissingCopy() {
        when(bookCopyRepository.findByCopyCode("missing")).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service().atualizar("missing",
                        request(UUID.randomUUID(), "ignored", "AVAILABLE", "A-01")))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("book.copy.not-found-by-code"));
    }

    @Test
    void deleteRejectsCopyOnActiveOrOverdueLoan() {
        BookCopy copy = BookCopy.builder().copyCode("T001").build();
        when(bookCopyRepository.findByCopyCode("T001")).thenReturn(Optional.of(copy));
        when(loanRepository.existsByBookCopy_CopyCodeAndStatusIn(
                "T001", List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE))).thenReturn(true);

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().excluir("T001"))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("book.copy.cannot-delete-active-loan"));
        verify(bookCopyRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesCopyWhenItHasNoActiveLoan() {
        BookCopy copy = BookCopy.builder().copyCode("T001").build();
        when(bookCopyRepository.findByCopyCode("T001")).thenReturn(Optional.of(copy));
        when(loanRepository.existsByBookCopy_CopyCodeAndStatusIn(
                "T001", List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE))).thenReturn(false);

        service().excluir("T001");

        verify(bookCopyRepository).delete(copy);
    }

    private BookCopyService service() {
        return new BookCopyService(bookCopyRepository, bookRepository, loanRepository);
    }

    private static BookCopyRequest request(UUID bookId, String copyCode, String status, String location) {
        return BookCopyRequest.builder()
                .bookId(bookId)
                .copyCode(copyCode)
                .status(status)
                .physicalLocation(location)
                .build();
    }
}
