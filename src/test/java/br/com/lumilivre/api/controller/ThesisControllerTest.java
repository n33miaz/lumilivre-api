package br.com.lumilivre.api.controller;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.mapper.ThesisMapper;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.service.ThesisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ThesisController.class)
@Import({I18nConfig.class, MessageResolver.class, ThesisMapper.class})
@WithMockUser(roles = "ADMIN")
class ThesisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ThesisService thesisService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void listReturnsOkWithContentLanguage() throws Exception {
        when(thesisService.listTheses(isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/theses").header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void studentCanList() throws Exception {
        when(thesisService.listTheses(isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/theses"))
                .andExpect(status().isOk());
    }

    @Test
    void searchReturnsOkWithContentLanguage() throws Exception {
        when(thesisService.searchTheses(isNull(), isNull(), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/theses/search").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"));
    }
}
