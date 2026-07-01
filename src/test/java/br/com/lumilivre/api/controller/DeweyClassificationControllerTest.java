package br.com.lumilivre.api.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.dto.dewey.DeweyClassificationResponse;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.service.DeweyClassificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DeweyClassificationController.class)
@Import({I18nConfig.class, MessageResolver.class})
@WithMockUser(roles = "ADMIN")
class DeweyClassificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeweyClassificationService deweyClassificationService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void listReturnsOkWithContentLanguage() throws Exception {
        when(deweyClassificationService.list()).thenReturn(List.of(
                new DeweyClassificationResponse("100", "Philosophy")));

        mockMvc.perform(get("/api/dewey-classifications").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"))
                .andExpect(jsonPath("$[0].code").value("100"))
                .andExpect(jsonPath("$[0].description").value("Philosophy"));
    }

    @Test
    @WithMockUser(roles = "READER")
    void readerCanList() throws Exception {
        when(deweyClassificationService.list()).thenReturn(List.of());

        mockMvc.perform(get("/api/dewey-classifications"))
                .andExpect(status().isOk());
    }
}
