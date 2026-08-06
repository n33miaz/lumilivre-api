package br.com.lumilivre.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.config.MethodSecuritySliceConfig;
import br.com.lumilivre.api.dto.studyshift.StudyShiftResponse;
import br.com.lumilivre.api.mapper.StudyShiftMapper;
import br.com.lumilivre.api.model.StudyShift;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.service.StudyShiftService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = StudyShiftController.class)
@Import({MethodSecuritySliceConfig.class, I18nConfig.class, MessageResolver.class})
@WithMockUser(roles = "ADMIN")
class StudyShiftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StudyShiftService studyShiftService;

    @MockBean
    private StudyShiftMapper mapper;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void listReturnsOkWithContentLanguage() throws Exception {
        when(studyShiftService.buscarPorTexto(isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/study-shifts").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"));
    }

    @Test
    void listReturnsPtBRContentLanguage() throws Exception {
        when(studyShiftService.buscarPorTexto(isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/study-shifts").header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"));
    }

    @Test
    void createReturns201() throws Exception {
        StudyShift entity = new StudyShift();
        entity.setId(1);
        entity.setName("Noturno");
        when(studyShiftService.cadastrar(any())).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(new StudyShiftResponse(1, "Noturno"));

        mockMvc.perform(post("/api/study-shifts").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Noturno\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        doNothing().when(studyShiftService).excluir(1);

        mockMvc.perform(delete("/api/study-shifts/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    /** Turno é audiência de comunicado: o leitor lê a lista, só a equipe escreve. */
    @Test
    @WithMockUser(roles = "READER")
    void aReaderReadsTheShiftListButDoesNotWriteToIt() throws Exception {
        when(studyShiftService.buscarPorTexto(isNull(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/study-shifts")).andExpect(status().isOk());

        mockMvc.perform(post("/api/study-shifts").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Madrugada\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/study-shifts/1").with(csrf()))
                .andExpect(status().isForbidden());

        verify(studyShiftService, never()).cadastrar(any());
        verify(studyShiftService, never()).excluir(anyInt());
    }
}
