package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import br.com.lumilivre.api.dto.book.BookCardResponse;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.BookCopy;
import br.com.lumilivre.api.model.Genre;
import br.com.lumilivre.api.model.Loan;
import br.com.lumilivre.api.repository.BookRepository;
import br.com.lumilivre.api.repository.LoanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    private static final String REGISTRATION_NUMBER = "2025001";

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookRepository bookRepository;

    @Captor
    private ArgumentCaptor<List<String>> genresCaptor;

    @Captor
    private ArgumentCaptor<List<UUID>> readBooksCaptor;

    @Test
    void recommendForStudentReturnsTopRatedWhenStudentHasNoHistory() {
        List<BookCardResponse> topRated = List.of(card("Clean Code"));
        when(loanRepository.findByStudent_RegistrationNumber(REGISTRATION_NUMBER)).thenReturn(List.of());
        when(bookRepository.findTopRated(PageRequest.of(0, 10))).thenReturn(topRated);

        List<BookCardResponse> result = service().recommendForStudent(REGISTRATION_NUMBER);

        assertThat(result).isSameAs(topRated);
        verify(bookRepository).findTopRated(PageRequest.of(0, 10));
    }

    @Test
    void recommendForStudentUsesPreferredGenresAndExcludesReadBooks() {
        UUID readBookId = UUID.randomUUID();
        List<BookCardResponse> recommendations = new ArrayList<>(List.of(card("Domain-Driven Design")));
        when(loanRepository.findByStudent_RegistrationNumber(REGISTRATION_NUMBER))
                .thenReturn(List.of(loan(readBookId, "Architecture")));
        when(bookRepository.findRecomendacoesPorGenero(
                genresCaptor.capture(),
                readBooksCaptor.capture(),
                eq(PageRequest.of(0, 10))))
                .thenReturn(recommendations);

        List<BookCardResponse> result = service().recommendForStudent(REGISTRATION_NUMBER);

        assertThat(genresCaptor.getValue()).containsExactly("architecture");
        assertThat(readBooksCaptor.getValue()).containsExactly(readBookId);
        assertThat(result).extracting(BookCardResponse::getTitle)
                .containsExactly("Domain-Driven Design");
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> result.add(card("Mutable List")));
    }

    @Test
    void recommendForStudentFallsBackToTopRatedWhenGenreRecommendationsAreEmpty() {
        List<BookCardResponse> topRated = List.of(card("Refactoring"));
        when(loanRepository.findByStudent_RegistrationNumber(REGISTRATION_NUMBER))
                .thenReturn(List.of(loan(UUID.randomUUID(), "Software Engineering")));
        when(bookRepository.findRecomendacoesPorGenero(anyList(), anyList(), eq(PageRequest.of(0, 10))))
                .thenReturn(List.of());
        when(bookRepository.findTopRated(PageRequest.of(0, 10))).thenReturn(topRated);

        List<BookCardResponse> result = service().recommendForStudent(REGISTRATION_NUMBER);

        assertThat(result).isSameAs(topRated);
    }

    private RecommendationService service() {
        return new RecommendationService(loanRepository, bookRepository);
    }

    private static BookCardResponse card(String title) {
        return BookCardResponse.builder()
                .id(UUID.randomUUID())
                .title(title)
                .author("Author")
                .rating(4.8)
                .coverUrl("https://example.test/cover.jpg")
                .build();
    }

    private static Loan loan(UUID bookId, String genreName) {
        Genre genre = new Genre(1, genreName);
        Book book = Book.builder()
                .id(bookId)
                .title("Already Read")
                .genres(Set.of(genre))
                .build();
        BookCopy copy = BookCopy.builder().book(book).build();
        return Loan.builder().bookCopy(copy).build();
    }
}
