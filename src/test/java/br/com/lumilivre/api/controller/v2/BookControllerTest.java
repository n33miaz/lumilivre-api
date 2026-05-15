package br.com.lumilivre.api.controller.v2;

import static org.mockito.ArgumentMatchers.any;
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
import br.com.lumilivre.api.dto.v1.livro.LivroDetalheResponse;
import br.com.lumilivre.api.dto.v1.livro.LivroListagemResponse;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.mapper.v2.BookMapper;
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
@Import({I18nConfig.class, MessageResolver.class, BookMapper.class, EnumLabelResolver.class})
@WithMockUser(roles = "ADMIN")
class BookControllerTest {

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
        LivroListagemResponse v1 = new LivroListagemResponse(
                BookCopyStatus.AVAILABLE, "T001", "978-0-7432-7356-5",
                "100.1", "Dom Quixote", "Romance", "Cervantes", "Alfaguara", null);
        when(bookService.buscarParaListaAdmin(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(v1)));

        mockMvc.perform(get("/api/v2/books").header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"))
                .andExpect(jsonPath("$.content[0].copyCode").value("T001"))
                .andExpect(jsonPath("$.content[0].title").value("Dom Quixote"))
                .andExpect(jsonPath("$.content[0].copyStatus.code").value("AVAILABLE"))
                .andExpect(jsonPath("$.content[0].copyStatus.label").value("Disponível"));
    }

    @Test
    void listReturnsEnUSLabels() throws Exception {
        LivroListagemResponse v1 = new LivroListagemResponse(
                BookCopyStatus.AVAILABLE, "T001", "978-0-7432-7356-5",
                "100.1", "Dom Quixote", "Romance", "Cervantes", "Alfaguara", null);
        when(bookService.buscarParaListaAdmin(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(v1)));

        mockMvc.perform(get("/api/v2/books").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"))
                .andExpect(jsonPath("$.content[0].copyStatus.code").value("AVAILABLE"))
                .andExpect(jsonPath("$.content[0].copyStatus.label").value("Available"));
    }

    @Test
    void getOneNotFoundReturnsI18nError() throws Exception {
        UUID id = UUID.randomUUID();
        when(bookService.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v2/books/{id}", id).header("Accept-Language", "en-US"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Language", "en-US"))
                .andExpect(jsonPath("$.message").value("Book not found."));
    }
}
