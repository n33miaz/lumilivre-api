package br.com.lumilivre.api.controller.v2;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.dto.v1.livro.LivroListagemResponse;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.mapper.v2.BookCopyMapper;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.service.BookCopyService;
import br.com.lumilivre.api.service.EnumLabelResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = BookCopyController.class)
@Import({I18nConfig.class, MessageResolver.class, BookCopyMapper.class, EnumLabelResolver.class})
@WithMockUser(roles = "ADMIN")
class BookCopyControllerTest {

    private static final UUID BOOK_ID = UUID.fromString("00000000-0000-0000-0000-000000000042");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookCopyService bookCopyService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void listByBookReturnsPtBRWithContentLanguage() throws Exception {
        LivroListagemResponse v1 = new LivroListagemResponse(
                BookCopyStatus.AVAILABLE, "T001", "978-0-7432-7356-5",
                "100.1", "Dom Quixote", "Romance", "Cervantes", "Alfaguara", "Estante A1");
        when(bookCopyService.buscarExemplaresPorLivroId(BOOK_ID)).thenReturn(List.of(v1));

        mockMvc.perform(get("/api/v2/book-copies/by-book/{bookId}", BOOK_ID)
                        .header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"))
                .andExpect(jsonPath("$[0].copyCode").value("T001"))
                .andExpect(jsonPath("$[0].title").value("Dom Quixote"))
                .andExpect(jsonPath("$[0].status.code").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].status.label").value("Disponível"))
                .andExpect(jsonPath("$[0].physicalLocation").value("Estante A1"));
    }

    @Test
    void listByBookReturnsEnUSLabels() throws Exception {
        LivroListagemResponse v1 = new LivroListagemResponse(
                BookCopyStatus.BORROWED, "T002", "978-0-7432-7356-5",
                "100.1", "Dom Quixote", "Romance", "Cervantes", "Alfaguara", "Estante A1");
        when(bookCopyService.buscarExemplaresPorLivroId(BOOK_ID)).thenReturn(List.of(v1));

        mockMvc.perform(get("/api/v2/book-copies/by-book/{bookId}", BOOK_ID)
                        .header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"))
                .andExpect(jsonPath("$[0].status.code").value("BORROWED"))
                .andExpect(jsonPath("$[0].status.label").value("Borrowed"));
    }

    @Test
    void listByBookReturnsNoContentWhenEmpty() throws Exception {
        when(bookCopyService.buscarExemplaresPorLivroId(BOOK_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/book-copies/by-book/{bookId}", BOOK_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void createReturns201() throws Exception {
        doNothing().when(bookCopyService).cadastrar(any());

        mockMvc.perform(post("/api/v2/book-copies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"copyCode":"T999","status":"AVAILABLE",
                                 "bookId":"%s","physicalLocation":"Estante B2"}
                                """.formatted(BOOK_ID)))
                .andExpect(status().isCreated());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        doNothing().when(bookCopyService).excluir("T001");

        mockMvc.perform(delete("/api/v2/book-copies/T001").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
