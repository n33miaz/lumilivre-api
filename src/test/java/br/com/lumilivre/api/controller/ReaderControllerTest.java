package br.com.lumilivre.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.config.MethodSecuritySliceConfig;
import br.com.lumilivre.api.dto.reader.ReaderListItem;
import br.com.lumilivre.api.dto.reader.ReaderPenaltySummaryResponse;
import br.com.lumilivre.api.mapper.ReaderMapper;
import br.com.lumilivre.api.model.Reader;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.security.ReaderAuthorizationService;
import br.com.lumilivre.api.service.EnumLabelResolver;
import br.com.lumilivre.api.service.ReaderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReaderController.class)
@Import({MethodSecuritySliceConfig.class, I18nConfig.class, MessageResolver.class, ReaderMapper.class, EnumLabelResolver.class})
@WithMockUser(roles = "ADMIN")
class ReaderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReaderService readerService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean(name = "readerAuthz")
    private ReaderAuthorizationService readerAuthorizationService;

    @Test
    void listReturnsPtBRByDefault() throws Exception {
        ReaderListItem item = new ReaderListItem(null, "12345", "Admin", null, "Joao Silva", null, null, null);
        when(readerService.listarParaAdminV2(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        mockMvc.perform(get("/api/readers").header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"))
                .andExpect(jsonPath("$.content[0].registrationNumber").value("12345"))
                .andExpect(jsonPath("$.content[0].fullName").value("Joao Silva"));
    }

    @Test
    void listReturnsEnUS() throws Exception {
        ReaderListItem item = new ReaderListItem(null, "12345", "Admin", null, "John Doe", null, null, null);
        when(readerService.listarParaAdminV2(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        mockMvc.perform(get("/api/readers").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"))
                .andExpect(jsonPath("$.content[0].registrationNumber").value("12345"));
    }

    @Test
    void penaltySummaryReturnsGlobalCounts() throws Exception {
        when(readerService.getPenaltySummary())
                .thenReturn(new ReaderPenaltySummaryResponse(41, 4, 2, 3));

        mockMvc.perform(get("/api/readers/penalty-summary").header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"))
                .andExpect(jsonPath("$.noPenalty").value(41))
                .andExpect(jsonPath("$.warning").value(4))
                .andExpect(jsonPath("$.suspension").value(2))
                .andExpect(jsonPath("$.block").value(3));
    }

    @Test
    void getOneSetsContentLanguageFromAcceptHeader() throws Exception {
        Reader reader = new Reader();
        reader.setRegistrationNumber("12345");
        reader.setFullName("Maria");
        when(readerAuthorizationService.canAccess("12345")).thenReturn(true);
        when(readerService.buscarPorMatricula(anyString())).thenReturn(reader);

        mockMvc.perform(get("/api/readers/12345").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"))
                .andExpect(jsonPath("$.registrationNumber").value("12345"))
                .andExpect(jsonPath("$.fullName").value("Maria"));
    }

    /**
     * A ficha do leitor é o alvo clássico de IDOR: trocar a matrícula na URL e
     * ler o cadastro do colega — nome, e-mail, telefone, endereço. Quem decide é
     * o {@code @CanAccessReader}; aqui o guarda diz "não" e o controller não
     * pode nem chegar ao service.
     */
    @Test
    void aReaderCannotOpenSomeoneElsesRecord() throws Exception {
        when(readerAuthorizationService.canAccess("99999")).thenReturn(false);

        mockMvc.perform(get("/api/readers/99999"))
                .andExpect(status().isForbidden());

        verify(readerService, never()).buscarPorMatricula(anyString());
    }

    /**
     * A listagem inteira é da equipe. Se o leitor alcançasse a lista, a barreira
     * por matrícula acima perderia a graça: bastaria pedir todo mundo de uma vez.
     */
    @Test
    @WithMockUser(roles = "READER")
    void aReaderCannotListEveryReader() throws Exception {
        mockMvc.perform(get("/api/readers"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/readers/penalty-summary"))
                .andExpect(status().isForbidden());

        verify(readerService, never()).listarParaAdminV2(any(), any(Pageable.class));
        verify(readerService, never()).getPenaltySummary();
    }
}
