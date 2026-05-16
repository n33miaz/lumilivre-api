package br.com.lumilivre.api.controller.v2;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.dto.common.AddressLookupResponse;
import br.com.lumilivre.api.repository.BookRepository;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.service.EnumLabelResolver;
import br.com.lumilivre.api.service.infra.CepService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MetadataController.class)
@Import({I18nConfig.class, MessageResolver.class, EnumLabelResolver.class})
@WithMockUser(roles = "ADMIN")
class MetadataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookRepository bookRepository;

    @MockBean
    private CepService cepService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void enumValuesAreLocalized() throws Exception {
        mockMvc.perform(get("/api/v2/metadata/enums/CLASSIFICACAO_ETARIA")
                        .header("Accept-Language", "en-US"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "en-US"))
                .andExpect(jsonPath("$[0].code").value("CHILDREN"))
                .andExpect(jsonPath("$[0].label").value("Children"));
    }

    @Test
    void authorsReturnsPagedSummaries() throws Exception {
        when(bookRepository.countByAutor()).thenReturn(List.of(
                Map.of("autor", "Machado de Assis", "total", 3L),
                Map.of("autor", "Clarice Lispector", "total", 2L)));

        mockMvc.perform(get("/api/v2/metadata/authors")
                        .param("q", "machado")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Machado de Assis"))
                .andExpect(jsonPath("$.content[0].totalBooks").value(3));
    }

    @Test
    void postalCodeMapsExternalAddress() throws Exception {
        AddressLookupResponse address = new AddressLookupResponse();
        address.setLogradouro("Praca da Se");
        address.setBairro("Se");
        address.setLocalidade("Sao Paulo");
        address.setUf("SP");
        when(cepService.buscarEnderecoPorCep("01001000")).thenReturn(address);

        mockMvc.perform(get("/api/v2/metadata/postal-codes/01001-000")
                        .header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"))
                .andExpect(jsonPath("$.postalCode").value("01001000"))
                .andExpect(jsonPath("$.street").value("Praca da Se"))
                .andExpect(jsonPath("$.city").value("Sao Paulo"))
                .andExpect(jsonPath("$.stateCode").value("SP"));
    }
}
