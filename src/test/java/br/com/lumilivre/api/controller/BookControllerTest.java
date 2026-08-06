package br.com.lumilivre.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.config.MethodSecuritySliceConfig;
import br.com.lumilivre.api.dto.book.BookListItemProjection;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.mapper.BookMapper;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.service.BookService;
import br.com.lumilivre.api.service.EnumLabelResolver;
import br.com.lumilivre.api.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = BookController.class)
@Import({MethodSecuritySliceConfig.class, I18nConfig.class, MessageResolver.class, BookMapper.class, EnumLabelResolver.class})
@WithMockUser(roles = "ADMIN")
class BookControllerTest {

    private static final UUID BOOK_ID = UUID.fromString("00000000-0000-4000-8000-000000003086");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @MockBean
    private RecommendationService recommendationService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void listReturnsPtBRWithEnFieldNames() throws Exception {
        BookListItemProjection projection = bookListItem(
                "AVAILABLE", "T001", "978-0-7432-7356-5", "100.1",
                "Dom Quixote", "Romance", "Cervantes", "Alfaguara", null);
        when(bookService.buscarParaListaAdmin(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(projection)));

        mockMvc.perform(get("/api/books").header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"))
                .andExpect(jsonPath("$.content[0].copyCode").value("T001"))
                .andExpect(jsonPath("$.content[0].title").value("Dom Quixote"))
                .andExpect(jsonPath("$.content[0].copyStatus.code").value("AVAILABLE"))
                .andExpect(jsonPath("$.content[0].copyStatus.label").value("Disponível"));
    }

    @Test
    void listReturnsEnUSLabels() throws Exception {
        BookListItemProjection projection = bookListItem(
                "AVAILABLE", "T001", "978-0-7432-7356-5", "100.1",
                "Dom Quixote", "Romance", "Cervantes", "Alfaguara", null);
        when(bookService.buscarParaListaAdmin(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(projection)));

        mockMvc.perform(get("/api/books").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"))
                .andExpect(jsonPath("$.content[0].copyStatus.code").value("AVAILABLE"))
                .andExpect(jsonPath("$.content[0].copyStatus.label").value("Available"));
    }

    @Test
    void getOneNotFoundReturnsI18nError() throws Exception {
        UUID id = UUID.randomUUID();
        when(bookService.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/books/{id}", id).header("Accept-Language", "en-US"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Language", "en-US"))
                .andExpect(jsonPath("$.message").value("Book not found."));
    }

    /**
     * SEC-15 ponta a ponta: sort recusado tem que sair como 400 no envelope
     * padrão e localizado — não como 500, que era o que o guarda interno do
     * Spring Data produzia.
     */
    @Test
    void maliciousSortReturnsLocalizedBadRequest() throws Exception {
        when(bookService.buscarParaListaAdmin(any(Pageable.class)))
                .thenThrow(BusinessRuleException.ofKey(
                        "error.sort.invalid-field", "id;DROP TABLE book--", "title, copyCode"));

        mockMvc.perform(get("/api/books")
                        .param("sort", "id;DROP TABLE book--")
                        .header("Accept-Language", "pt-BR"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Language", "pt-BR"))
                .andExpect(jsonPath("$.message")
                        .value("Campo de ordenação inválido: 'id;DROP TABLE book--'. Campos aceitos: title, copyCode."));
    }

    @Test
    void invalidUuidPathVariableReturnsBadRequestNotServerError() throws Exception {
        mockMvc.perform(get("/api/books/{id}", "nao-e-uuid").header("Accept-Language", "en-US"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations.id").value("Invalid value for this parameter."));
    }

    /**
     * A fronteira do catálogo em um teste: a ficha de um livro é pública (o app
     * abre antes do login), a <b>listagem</b> não é. Elas moram no mesmo
     * controller e diferem só na anotação — trocar {@code permitAll()} de linha
     * abriria tombo, prateleira e status de exemplar para qualquer aluno.
     */
    @Test
    @WithMockUser(roles = "READER")
    void aReaderReadsTheBookRecordButNotTheAdminListing() throws Exception {
        Book book = new Book();
        book.setId(BOOK_ID);
        book.setTitle("Dom Casmurro");
        when(bookService.findById(BOOK_ID)).thenReturn(Optional.of(book));

        mockMvc.perform(get("/api/books/{id}", BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Dom Casmurro"));

        mockMvc.perform(get("/api/books")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/books/grouped")).andExpect(status().isForbidden());

        verify(bookService, never()).buscarParaListaAdmin(any(Pageable.class));
    }

    private BookListItemProjection bookListItem(
            String status, String copyCode, String isbn, String deweyCode,
            String title, String genre, String author, String publisher, String physicalLocation) {
        return new BookListItemProjection() {
            @Override public String getStatus() { return status; }
            @Override public String getCopyCode() { return copyCode; }
            @Override public String getIsbn() { return isbn; }
            @Override public String getDeweyCode() { return deweyCode; }
            @Override public String getTitle() { return title; }
            @Override public String getGenre() { return genre; }
            @Override public String getAuthor() { return author; }
            @Override public String getPublisher() { return publisher; }
            @Override public String getPhysicalLocation() { return physicalLocation; }
        };
    }
}
