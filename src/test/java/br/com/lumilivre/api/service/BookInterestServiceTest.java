package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.lumilivre.api.dto.book.BookInterestResponse;
import br.com.lumilivre.api.dto.book.BookInterestStateResponse;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.mapper.BookMapper;
import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.BookInterest;
import br.com.lumilivre.api.model.Reader;
import br.com.lumilivre.api.repository.BookInterestRepository;
import br.com.lumilivre.api.repository.BookRepository;
import br.com.lumilivre.api.security.CustomUserDetails;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookInterestServiceTest {

    private static final UUID BOOK_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID READER_ID = UUID.fromString("00000000-0000-4000-8000-0000000000aa");
    private static final UUID OTHER_READER_ID = UUID.fromString("00000000-0000-4000-8000-0000000000bb");

    @Mock
    private BookInterestRepository bookInterestRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private EnumLabelResolver enumLabels;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void markingTwiceIsIdempotentAndKeepsTheFirstTimestamp() {
        OffsetDateTime firstTime = OffsetDateTime.parse("2026-03-01T10:00:00Z");
        authenticateReader(READER_ID);
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book()));
        when(bookInterestRepository.findByReader_IdAndBook_Id(READER_ID, BOOK_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(interest(firstTime)));
        when(bookInterestRepository.saveAndFlush(any(BookInterest.class)))
                .thenAnswer(invocation -> {
                    BookInterest saved = invocation.getArgument(0);
                    saved.setCreatedAt(firstTime);
                    return saved;
                });

        BookInterestStateResponse first = service().marcar(BOOK_ID);
        BookInterestStateResponse second = service().marcar(BOOK_ID);

        assertThat(first).isEqualTo(new BookInterestStateResponse(BOOK_ID, true, firstTime));
        assertThat(second).isEqualTo(first);
        // A segunda chamada nao insere: o unico INSERT foi o da primeira.
        verify(bookInterestRepository).saveAndFlush(any(BookInterest.class));
    }

    /**
     * Duplo toque quase simultaneo passa os dois pelo SELECT e o segundo INSERT
     * bate na unicidade da V8. Sem tratar, isso seria 500 num caminho trivial de
     * app movel.
     */
    @Test
    void concurrentDoubleTapDoesNotBecomeAServerError() {
        OffsetDateTime winnerTime = OffsetDateTime.parse("2026-03-01T10:00:00Z");
        authenticateReader(READER_ID);
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book()));
        when(bookInterestRepository.findByReader_IdAndBook_Id(READER_ID, BOOK_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(interest(winnerTime)));
        when(bookInterestRepository.saveAndFlush(any(BookInterest.class)))
                .thenThrow(new DataIntegrityViolationException("uq_book_interest_reader_book"));

        BookInterestStateResponse state = service().marcar(BOOK_ID);

        assertThat(state.interested()).isTrue();
        assertThat(state.markedAt()).isEqualTo(winnerTime);
    }

    @Test
    void markingAnUnknownBookIsNotFound() {
        authenticateReader(READER_ID);
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service().marcar(BOOK_ID))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("book.not-found"));
        verify(bookInterestRepository, never()).saveAndFlush(any());
    }

    /**
     * O leitor nunca vem do cliente: o interesse gravado carrega o leitor do
     * principal, entao nao existe corpo, query ou path em que trocar o dono. É
     * assim que o IDOR morre — por ausencia de superficie, nao por validacao.
     */
    @Test
    void theInterestAlwaysBelongsToThePrincipalAndNotToWhoeverIsAsked() {
        authenticateReader(READER_ID);
        when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book()));
        when(bookInterestRepository.findByReader_IdAndBook_Id(READER_ID, BOOK_ID)).thenReturn(Optional.empty());
        when(bookInterestRepository.saveAndFlush(any(BookInterest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service().marcar(BOOK_ID);

        verify(bookInterestRepository).saveAndFlush(org.mockito.ArgumentMatchers
                .argThat(interest -> READER_ID.equals(interest.getReader().getId())));
        verify(bookInterestRepository, never()).findByReader_IdAndBook_Id(eq(OTHER_READER_ID), any());
    }

    @Test
    void removingWhatWasNotMarkedIsNotAnError() {
        authenticateReader(READER_ID);
        when(bookRepository.existsById(BOOK_ID)).thenReturn(true);
        when(bookInterestRepository.deleteByReaderAndBook(READER_ID, BOOK_ID)).thenReturn(0);

        BookInterestStateResponse state = service().desmarcar(BOOK_ID);

        assertThat(state).isEqualTo(new BookInterestStateResponse(BOOK_ID, false, null));
    }

    @Test
    void removingOnlyEverTouchesThePrincipalsOwnRow() {
        authenticateReader(READER_ID);
        when(bookRepository.existsById(BOOK_ID)).thenReturn(true);
        when(bookInterestRepository.deleteByReaderAndBook(READER_ID, BOOK_ID)).thenReturn(1);

        service().desmarcar(BOOK_ID);

        verify(bookInterestRepository).deleteByReaderAndBook(READER_ID, BOOK_ID);
        verify(bookInterestRepository, never()).deleteByReaderAndBook(eq(OTHER_READER_ID), any());
    }

    @Test
    void staffHasNoReaderAndThereforeNoInterestToMark() {
        authenticateStaff();

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().marcar(BOOK_ID))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("interest.reader-required"));
    }

    @Test
    void anonymousCannotMarkInterest() {
        SecurityContextHolder.clearContext();

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().marcar(BOOK_ID))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("interest.reader-required"));
    }

    @Test
    void theReadersOwnListCarriesTheCardAndTheMarkDate() {
        OffsetDateTime markedAt = OffsetDateTime.parse("2026-03-01T10:00:00Z");
        authenticateReader(READER_ID);
        when(bookInterestRepository.findMine(eq(READER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(interest(markedAt))));

        List<BookInterestResponse> content =
                service().listarDoLeitorAutenticado(PageRequest.of(0, 20)).getContent();

        assertThat(content).hasSize(1);
        assertThat(content.get(0).markedAt()).isEqualTo(markedAt);
        assertThat(content.get(0).book().getTitle()).isEqualTo("Dom Casmurro");
        // updatedAt viaja no card: e o que permite o app invalidar o cache da capa.
        assertThat(content.get(0).book().getUpdatedAt()).isNotNull();
    }

    /**
     * As duas consultas tem ORDER BY escrito no repositorio, e em JPQL o Spring
     * Data anexa o sort do cliente ao final — o que daria 500 para qualquer campo.
     */
    @Test
    void clientSortIsDroppedBeforeReachingTheQuery() {
        authenticateReader(READER_ID);
        when(bookInterestRepository.findMine(eq(READER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(bookInterestRepository.summarize(any(), org.mockito.ArgumentMatchers.anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service().listarDoLeitorAutenticado(PageRequest.of(0, 20, Sort.by("campoInexistente")));
        service().resumir(false, PageRequest.of(0, 20, Sort.by("interestCount")));

        verify(bookInterestRepository).findMine(eq(READER_ID),
                org.mockito.ArgumentMatchers.argThat(page -> page.getSort().isUnsorted()));
        verify(bookInterestRepository).summarize(eq(BookCopyStatus.AVAILABLE), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.argThat(page -> page.getSort().isUnsorted()));
    }

    /**
     * O filtro "so o que nao conseguimos atender" viaja como teto numerico, e nao
     * como booleano: parametro booleano solto dentro de HAVING chega no Postgres
     * sem tipo e derruba a consulta.
     */
    @Test
    void unmetOnlyBecomesAZeroCeilingAndTheDefaultBecomesNoCeiling() {
        authenticateStaff();
        when(bookInterestRepository.summarize(any(), org.mockito.ArgumentMatchers.anyLong(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service().resumir(true, PageRequest.of(0, 20));
        verify(bookInterestRepository).summarize(BookCopyStatus.AVAILABLE, 0L, PageRequest.of(0, 20));

        service().resumir(false, PageRequest.of(0, 20));
        verify(bookInterestRepository).summarize(BookCopyStatus.AVAILABLE, Long.MAX_VALUE, PageRequest.of(0, 20));
    }

    private BookInterestService service() {
        return new BookInterestService(bookInterestRepository, bookRepository, new BookMapper(enumLabels));
    }

    private static Book book() {
        Book book = new Book();
        book.setId(BOOK_ID);
        book.setTitle("Dom Casmurro");
        book.setAuthor("Machado de Assis");
        book.setUpdatedAt(OffsetDateTime.parse("2026-02-01T08:00:00Z"));
        return book;
    }

    private static BookInterest interest(OffsetDateTime createdAt) {
        return BookInterest.builder()
                .id(UUID.randomUUID())
                .reader(reader(READER_ID))
                .book(book())
                .createdAt(createdAt)
                .build();
    }

    private static Reader reader(UUID id) {
        Reader reader = new Reader();
        reader.setId(id);
        reader.setRegistrationNumber("2024001");
        return reader;
    }

    private void authenticateReader(UUID readerId) {
        AppUser user = new AppUser();
        user.setRole(Role.READER);
        user.setReader(reader(readerId));
        authenticate(user);
    }

    private void authenticateStaff() {
        AppUser user = new AppUser();
        user.setRole(Role.LIBRARIAN);
        authenticate(user);
    }

    private void authenticate(AppUser user) {
        CustomUserDetails details = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    }
}
