package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import br.com.lumilivre.api.dto.book.BookGroupedResponse;
import br.com.lumilivre.api.dto.book.BookListItemProjection;
import br.com.lumilivre.api.dto.book.BookRequest;
import br.com.lumilivre.api.enums.AgeRating;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.CoverType;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.DeweyClassification;
import br.com.lumilivre.api.model.Genre;
import br.com.lumilivre.api.repository.BookCopyRepository;
import br.com.lumilivre.api.repository.BookRepository;
import br.com.lumilivre.api.repository.DeweyClassificationRepository;
import br.com.lumilivre.api.repository.GenreRepository;
import br.com.lumilivre.api.service.infra.bookmetadata.BookMetadata;
import br.com.lumilivre.api.service.infra.bookmetadata.BookMetadataChain;
import br.com.lumilivre.api.service.infra.storage.StorageBucket;
import br.com.lumilivre.api.service.infra.storage.StorageProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private StorageProvider storageProvider;

    @Mock
    private BookMetadataChain bookMetadataChain;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private DeweyClassificationRepository deweyClassificationRepository;

    @Mock
    private MultipartFile coverFile;

    @Captor
    private ArgumentCaptor<Book> bookCaptor;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @Test
    void advancedSearchNormalizesFiltersAndParsesEnums() {
        var pageable = PageRequest.of(0, 10);
        var page = new PageImpl<BookGroupedResponse>(List.of());
        LocalDate publicationDate = LocalDate.of(2024, 5, 1);
        when(bookRepository.buscarAvancado(
                eq("%clean code%"),
                eq("9780132350884"),
                eq("%martin%"),
                eq("%software%"),
                eq("%prentice%"),
                eq("005.1"),
                eq(AgeRating.GENERAL),
                eq(CoverType.SOFTCOVER),
                eq(publicationDate),
                eq(pageable))).thenReturn(page);

        assertThat(service().buscarAvancado(
                " Clean Code ",
                " 9780132350884 ",
                " Martin ",
                " Software ",
                " Prentice ",
                " 005.1 ",
                "general",
                "softcover",
                publicationDate,
                pageable)).isSameAs(page);
    }

    // ---- SEC-15: sort da query nativa ----------------------------------------

    @Test
    void adminListRejectsSortFieldOutsideTheAllowlist() {
        var malicioso = PageRequest.of(0, 20, Sort.by("id;DROP TABLE book--"));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().buscarParaListaAdmin(malicioso))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("error.sort.invalid-field"));

        verify(bookRepository, never()).findLivrosParaListaAdmin(any());
    }

    @Test
    void adminListRejectsRawColumnNameEvenWhenItExists() {
        // copy_code e o nome real da coluna e ordenava de verdade antes da
        // allowlist — prova de que o texto do cliente entrava na query.
        var pageable = PageRequest.of(0, 20, Sort.by("copy_code"));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().buscarParaListaAdmin(pageable));

        verify(bookRepository, never()).findLivrosParaListaAdmin(any());
    }

    @Test
    void adminListTranslatesAllowedSortFieldToColumn() {
        var page = new PageImpl<BookListItemProjection>(List.of());
        when(bookRepository.findLivrosParaListaAdmin(any(Pageable.class))).thenReturn(page);

        service().buscarParaListaAdmin(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "title")));

        verify(bookRepository).findLivrosParaListaAdmin(pageableCaptor.capture());
        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("l.title");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void textSearchSanitizesSortToo() {
        var malicioso = PageRequest.of(0, 20, Sort.by("id;DROP TABLE book--"));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().buscarPorTexto("dom", malicioso));

        verify(bookRepository, never()).findLivrosParaListaAdmin(any());
    }

    @Test
    void mobileCatalogGroupsBooksByGenreAndSortsGenres() {
        UUID architectureBookId = UUID.randomUUID();
        UUID literatureBookId = UUID.randomUUID();
        when(bookRepository.findCatalogoMobile()).thenReturn(List.of(
                row("Literature", literatureBookId, "Dom Casmurro", "Machado", "cover-2", null),
                row("Architecture", architectureBookId, "Patterns", "Alexander", "cover-1", 4.9)));

        var catalog = service().buscarCatalogoParaMobile();

        assertThat(catalog).extracting("genreName").containsExactly("Architecture", "Literature");
        assertThat(catalog.get(0).getBooks()).extracting("id").containsExactly(architectureBookId);
        assertThat(catalog.get(1).getBooks()).extracting("rating").containsExactly(4.6);
    }

    @Test
    void createBookRejectsDuplicateIsbnBeforeExternalLookup() {
        BookRequest request = request()
                .isbn("9780132350884")
                .build();
        when(bookRepository.findByIsbn("9780132350884")).thenReturn(Optional.of(Book.builder().build()));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().cadastrar(request, null))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("book.isbn.already-exists"));
        verify(bookMetadataChain, never()).findByIsbn(any());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void createBookFillsMissingFieldsFromMetadataAndMapsRelations() {
        BookRequest request = BookRequest.builder()
                .isbn("9780132350884")
                .ageRating("general")
                .coverType("softcover")
                .deweyCode("005.1")
                .genres(Set.of("Software", "Architecture"))
                .build();
        BookMetadata metadata = BookMetadata.builder("test-provider")
                .title("Clean Code")
                .author("Robert C. Martin")
                .publisher("Prentice Hall")
                .synopsis("Practical software craftsmanship")
                .publicationDate(LocalDate.of(2008, 8, 1))
                .pageCount(464)
                .coverUrl("https://cdn.test/clean-code.jpg")
                .rating(4.8)
                .build();
        DeweyClassification dewey = new DeweyClassification("005.1", "Programming");
        Genre software = new Genre(1, "Software");
        Genre architecture = new Genre(2, "Architecture");
        when(bookRepository.findByIsbn("9780132350884")).thenReturn(Optional.empty());
        when(bookMetadataChain.findByIsbn("9780132350884")).thenReturn(Optional.of(metadata));
        when(deweyClassificationRepository.findById("005.1")).thenReturn(Optional.of(dewey));
        when(genreRepository.findByNameIn(Set.of("Software", "Architecture")))
                .thenReturn(Set.of(software, architecture));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Book result = service().cadastrar(request, null);

        assertThat(result.getIsbn()).isEqualTo("9780132350884");
        assertThat(result.getTitle()).isEqualTo("Clean Code");
        assertThat(result.getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(result.getPublisher()).isEqualTo("Prentice Hall");
        assertThat(result.getSynopsis()).isEqualTo("Practical software craftsmanship");
        assertThat(result.getPublicationDate()).isEqualTo(LocalDate.of(2008, 8, 1));
        assertThat(result.getPageCount()).isEqualTo(464);
        assertThat(result.getCoverUrl()).isEqualTo("https://cdn.test/clean-code.jpg");
        assertThat(result.getRating()).isEqualTo(4.8);
        assertThat(result.getAgeRating()).isEqualTo(AgeRating.GENERAL);
        assertThat(result.getCoverType()).isEqualTo(CoverType.SOFTCOVER);
        assertThat(result.getDeweyClassification()).isSameAs(dewey);
        assertThat(result.getGenres()).containsExactlyInAnyOrder(software, architecture);
    }

    @Test
    void createBookRejectsFuturePublicationDate() {
        BookRequest request = request()
                .publicationDate(LocalDate.now().plusDays(1))
                .build();

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().cadastrar(request, null))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("book.form.publication-date.future-not-allowed"));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void createBookWrapsCoverUploadFailure() {
        when(coverFile.isEmpty()).thenReturn(false);
        when(storageProvider.upload(coverFile, StorageBucket.COVERS)).thenThrow(new RuntimeException("storage down"));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().cadastrar(request().build(), coverFile))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("book.cover.upload-failed"));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void updateBookRejectsIsbnAlreadyOwnedByAnotherBook() {
        UUID id = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        when(bookRepository.findById(id)).thenReturn(Optional.of(Book.builder()
                .id(id)
                .isbn("old-isbn")
                .build()));
        when(bookRepository.findByIsbn("new-isbn")).thenReturn(Optional.of(Book.builder()
                .id(otherId)
                .isbn("new-isbn")
                .build()));

        assertThatExceptionOfType(BusinessRuleException.class)
                .isThrownBy(() -> service().atualizar(id, request().isbn("new-isbn").build(), null))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("book.isbn.belongs-to-other"));
        verify(bookRepository, never()).save(any());
    }

    @Test
    void uploadCoverStoresProviderUrl() {
        UUID id = UUID.randomUUID();
        Book book = Book.builder().id(id).title("Clean Code").build();
        when(bookRepository.findById(id)).thenReturn(Optional.of(book));
        when(coverFile.isEmpty()).thenReturn(false);
        when(storageProvider.upload(coverFile, StorageBucket.COVERS)).thenReturn("https://cdn.test/new-cover.jpg");
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Book result = service().uploadCapa(id, coverFile);

        assertThat(result.getCoverUrl()).isEqualTo("https://cdn.test/new-cover.jpg");
    }

    @Test
    void uploadCoverRejectsMissingBook() {
        UUID id = UUID.randomUUID();
        when(bookRepository.findById(id)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service().uploadCapa(id, coverFile))
                .satisfies(error -> assertThat(error.getMessageKey()).isEqualTo("book.not-found-for-id"));
        verify(storageProvider, never()).upload(any(), any());
    }

    @Test
    void deleteBookRemovesCopiesBeforeBook() {
        UUID id = UUID.randomUUID();
        when(bookRepository.existsById(id)).thenReturn(true);

        service().excluirLivroComExemplares(id);

        verify(bookCopyRepository).deleteAllByBook_Id(id);
        verify(bookRepository).deleteById(id);
    }

    @Test
    void searchByIsbnReturnsRequestFilledFromMetadata() {
        when(bookMetadataChain.findByIsbn("9780132350884")).thenReturn(Optional.of(BookMetadata.builder("test-provider")
                .title("Clean Code")
                .author("Robert C. Martin")
                .publisher("Prentice Hall")
                .rating(4.7)
                .build()));

        BookRequest result = service().pesquisarDadosPorIsbn("9780132350884");

        assertThat(result.getIsbn()).isEqualTo("9780132350884");
        assertThat(result.getTitle()).isEqualTo("Clean Code");
        assertThat(result.getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(result.getPublisher()).isEqualTo("Prentice Hall");
        assertThat(result.getRating()).isEqualTo(4.7);
    }

    @Test
    void searchByIsbnRejectsMissingExternalMetadata() {
        when(bookMetadataChain.findByIsbn("missing")).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service().pesquisarDadosPorIsbn("missing"))
                .satisfies(error -> assertThat(error.getMessageKey())
                        .isEqualTo("book.external.not-found-for-isbn"));
    }

    @Test
    void copyCountersDelegateToRepositories() {
        UUID id = UUID.randomUUID();
        when(bookCopyRepository.countByBookIdAndStatus(id, BookCopyStatus.AVAILABLE)).thenReturn(3L);
        when(bookCopyRepository.countByBook_Id(id)).thenReturn(5L);

        assertThat(service().countAvailableCopies(id)).isEqualTo(3);
        assertThat(service().countTotalCopies(id)).isEqualTo(5);
    }

    private BookService service() {
        return new BookService(
                bookCopyRepository,
                bookRepository,
                storageProvider,
                bookMetadataChain,
                genreRepository,
                deweyClassificationRepository);
    }

    private static BookRequest.BookRequestBuilder request() {
        return BookRequest.builder()
                .isbn("9780132350884")
                .title("Clean Code")
                .author("Robert C. Martin")
                .publisher("Prentice Hall")
                .publicationDate(LocalDate.of(2008, 8, 1))
                .pageCount(464)
                .ageRating("general")
                .coverType("softcover")
                .rating(4.8);
    }

    private static Map<String, Object> row(
            String genreName, UUID id, String title, String author, String coverUrl, Double rating) {
        Map<String, Object> row = new HashMap<>();
        row.put("genrename", genreName);
        row.put("id", id);
        row.put("title", title);
        row.put("author", author);
        row.put("coverurl", coverUrl);
        row.put("rating", rating);
        return row;
    }
}
