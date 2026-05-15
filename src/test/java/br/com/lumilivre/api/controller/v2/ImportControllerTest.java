package br.com.lumilivre.api.controller.v2;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.service.ImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ImportController.class)
@Import({I18nConfig.class, MessageResolver.class})
@WithMockUser(roles = "ADMIN")
class ImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImportService importService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void importStudentsReturnsOkForAdmin() throws Exception {
        when(importService.importar(eq("aluno"), any(), any())).thenReturn("10 alunos importados");

        MockMultipartFile file = new MockMultipartFile("file", "students.csv",
                "text/csv", "matricula,nome\n12345,João".getBytes());

        mockMvc.perform(multipart("/api/v2/imports/students").file(file).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void importBooksReturnsOkForAdmin() throws Exception {
        when(importService.importar(eq("livro"), any(), any())).thenReturn("5 livros importados");

        MockMultipartFile file = new MockMultipartFile("file", "books.csv",
                "text/csv", "isbn,titulo\n9780001,Dom Quixote".getBytes());

        mockMvc.perform(multipart("/api/v2/imports/books").file(file).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void importCopiesReturnsOkForAdmin() throws Exception {
        when(importService.importar(eq("exemplar"), any(), any())).thenReturn("3 exemplares importados");

        MockMultipartFile file = new MockMultipartFile("file", "copies.csv",
                "text/csv", "tombo,livro_id\nT001,uuid-here".getBytes());

        mockMvc.perform(multipart("/api/v2/imports/copies").file(file).with(csrf()))
                .andExpect(status().isOk());
    }
}
