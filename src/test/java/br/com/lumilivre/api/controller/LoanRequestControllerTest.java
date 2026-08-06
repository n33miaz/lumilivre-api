package br.com.lumilivre.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.config.MethodSecuritySliceConfig;
import br.com.lumilivre.api.mapper.LoanRequestMapper;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.security.ReaderAuthorizationService;
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
@Import({MethodSecuritySliceConfig.class, I18nConfig.class, MessageResolver.class, LoanRequestMapper.class, EnumLabelResolver.class})
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

    @MockBean(name = "readerAuthz")
    private ReaderAuthorizationService readerAuthorizationService;

    @Test
    void listAllReturnsPtBRContentLanguage() throws Exception {
        when(loanRequestService.listAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/loan-requests").header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"));
    }

    @Test
    void listAllReturnsEnUSContentLanguage() throws Exception {
        when(loanRequestService.listAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/loan-requests").header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createResolvesSuccessMessageInPtBr() throws Exception {
        when(readerAuthorizationService.canAccess("12345")).thenReturn(true);
        when(loanRequestService.solicitarEmprestimo("12345", "T001"))
                .thenReturn("request.created");

        mockMvc.perform(post("/api/loan-requests").with(csrf())
                        .param("readerRegistrationNumber", "12345")
                        .param("copyCode", "T001")
                        .header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"))
                .andExpect(content().string("Solicitação registrada com sucesso."));
    }

    @Test
    void processResolvesSuccessMessageInEnUs() throws Exception {
        String requestId = "00000000-0000-0000-0000-000000000007";
        when(loanRequestService.processarSolicitacao(UUID.fromString(requestId), true))
                .thenReturn("request.processed");

        mockMvc.perform(post("/api/loan-requests/{id}/process", requestId).with(csrf())
                        .param("accept", "true")
                        .header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"))
                .andExpect(content().string("Loan request processed successfully."));
    }

    /**
     * Solicitar empréstimo em nome de outra matrícula é o caminho mais curto
     * para atribuir uma dívida a um colega. Quem barra é o
     * {@code @readerAuthz.canAccess(#readerRegistrationNumber)} — e ele não pode
     * ser só decoração.
     */
    @Test
    @WithMockUser(roles = "READER")
    void aReaderCannotRequestALoanForAnotherRegistrationNumber() throws Exception {
        when(readerAuthorizationService.canAccess("99999")).thenReturn(false);

        mockMvc.perform(post("/api/loan-requests").with(csrf())
                        .param("readerRegistrationNumber", "99999")
                        .param("copyCode", "T001"))
                .andExpect(status().isForbidden());

        verify(loanRequestService, never()).solicitarEmprestimo(anyString(), anyString());
    }

    /** Aprovar/recusar é da equipe: o próprio solicitante não decide o pedido. */
    @Test
    @WithMockUser(roles = "READER")
    void aReaderCannotApproveTheirOwnRequest() throws Exception {
        mockMvc.perform(post("/api/loan-requests/{id}/process",
                        "00000000-0000-0000-0000-000000000007").with(csrf())
                        .param("accept", "true"))
                .andExpect(status().isForbidden());

        verify(loanRequestService, never()).processarSolicitacao(any(UUID.class), anyBoolean());
    }
}
