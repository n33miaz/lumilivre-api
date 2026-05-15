package br.com.lumilivre.api.controller.v2;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.model.Genre;
import br.com.lumilivre.api.repository.GenreRepository;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = GenreController.class)
@Import({I18nConfig.class, MessageResolver.class})
@WithMockUser(roles = "ADMIN")
class GenreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GenreRepository genreRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void listReturnsOkWithContentLanguage() throws Exception {
        Genre genre = new Genre();
        genre.setId(1);
        genre.setName("Romance");
        when(genreRepository.findAll()).thenReturn(List.of(genre));

        mockMvc.perform(get("/api/v2/genres").header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Romance"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCanList() throws Exception {
        when(genreRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/genres"))
                .andExpect(status().isOk());
    }
}
