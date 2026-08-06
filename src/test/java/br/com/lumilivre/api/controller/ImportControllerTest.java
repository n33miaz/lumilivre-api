package br.com.lumilivre.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.config.MethodSecuritySliceConfig;
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
@Import({MethodSecuritySliceConfig.class, I18nConfig.class, MessageResolver.class})
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
    void importReadersReturnsOkForAdmin() throws Exception {
        when(importService.importar(eq("leitor"), any(), any())).thenReturn("10 leitores importados");

        MockMultipartFile file = new MockMultipartFile("file", "readers.csv",
                "text/csv", "matricula,nome\n12345,João".getBytes());

        mockMvc.perform(multipart("/api/imports/readers").file(file).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void importBooksReturnsOkForAdmin() throws Exception {
        when(importService.importar(eq("livro"), any(), any())).thenReturn("5 livros importados");

        MockMultipartFile file = new MockMultipartFile("file", "books.csv",
                "text/csv", "isbn,titulo\n9780001,Dom Quixote".getBytes());

        mockMvc.perform(multipart("/api/imports/books").file(file).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void importCopiesReturnsOkForAdmin() throws Exception {
        when(importService.importar(eq("exemplar"), any(), any())).thenReturn("3 exemplares importados");

        MockMultipartFile file = new MockMultipartFile("file", "copies.csv",
                "text/csv", "tombo,livro_id\nT001,uuid-here".getBytes());

        mockMvc.perform(multipart("/api/imports/copies").file(file).with(csrf()))
                .andExpect(status().isOk());
    }

    /**
     * Importação em massa é só do ADMIN — inclusive para o bibliotecário. Uma
     * planilha errada reescreve o acervo inteiro de uma vez, e não existe
     * "desfazer": é a operação com o maior estrago por clique do sistema.
     */
    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void aLibrarianCannotBulkImport() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "readers.csv",
                "text/csv", "matricula,nome\n12345,João".getBytes());

        mockMvc.perform(multipart("/api/imports/readers").file(file).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/imports/books").file(file).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/imports/copies").file(file).with(csrf()))
                .andExpect(status().isForbidden());

        verify(importService, never()).importar(anyString(), any(), any());
    }
}
