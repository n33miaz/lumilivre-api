package br.com.lumilivre.api.controller.v2;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.mapper.v2.AcademicModuleMapper;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.service.AcademicModuleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AcademicModuleController.class)
@Import({I18nConfig.class, MessageResolver.class})
@WithMockUser(roles = "ADMIN")
class AcademicModuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AcademicModuleService academicModuleService;

    @MockBean
    private AcademicModuleMapper mapper;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void listReturnsOkWithContentLanguage() throws Exception {
        when(academicModuleService.buscarPorTexto(isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v2/academic-modules").header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"));
    }

    @Test
    void listReturnsEnUSContentLanguage() throws Exception {
        when(academicModuleService.buscarPorTexto(isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/v2/academic-modules").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"));
    }

    @Test
    void createReturns201() throws Exception {
        when(academicModuleService.cadastrar(any()))
                .thenReturn(ResponseEntity.status(201).body(null));

        mockMvc.perform(post("/api/v2/academic-modules").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Módulo A\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        when(academicModuleService.excluir(1)).thenReturn(null);

        mockMvc.perform(delete("/api/v2/academic-modules/1").with(csrf()))
                .andExpect(status().isNoContent());
    }
}
