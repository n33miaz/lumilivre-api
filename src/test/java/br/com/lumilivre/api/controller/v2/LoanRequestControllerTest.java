package br.com.lumilivre.api.controller.v2;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.dto.v1.solicitacao.SolicitacaoCompletaResponse;
import br.com.lumilivre.api.mapper.v2.LoanRequestMapper;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.security.StudentAuthorizationService;
import br.com.lumilivre.api.service.EnumLabelResolver;
import br.com.lumilivre.api.service.LoanRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LoanRequestController.class)
@Import({I18nConfig.class, MessageResolver.class, LoanRequestMapper.class, EnumLabelResolver.class})
@WithMockUser(roles = "ADMIN")
class LoanRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoanRequestService loanRequestService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private StudentAuthorizationService studentAuthorizationService;

    @Test
    void listAllReturnsPtBRContentLanguage() throws Exception {
        SolicitacaoCompletaResponse item = new SolicitacaoCompletaResponse();
        item.setId(UUID.randomUUID());
        when(loanRequestService.listarTodasSolicitacoes()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v2/loan-requests").header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"));
    }

    @Test
    void listAllReturnsEnUSContentLanguage() throws Exception {
        when(loanRequestService.listarTodasSolicitacoes()).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/loan-requests").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"))
                .andExpect(jsonPath("$").isArray());
    }
}
