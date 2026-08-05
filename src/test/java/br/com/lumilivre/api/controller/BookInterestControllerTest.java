package br.com.lumilivre.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.dto.book.BookCardResponse;
import br.com.lumilivre.api.dto.book.BookInterestResponse;
import br.com.lumilivre.api.dto.book.BookInterestStateResponse;
import br.com.lumilivre.api.dto.book.BookInterestSummaryResponse;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.service.BookInterestService;

/**
 * Slice das quatro rotas de interesse: o contrato que web (T12) e app (T21) vao
 * consumir, e as barreiras de papel do {@code @PreAuthorize}.
 *
 * <p>A barreira de URL do {@code SecurityConfig} nao existe num slice de
 * {@code @WebMvcTest}; ela e verificada em
 * {@code PublicEndpointsAccessTest} (convidado) e no teste de integracao contra
 * Postgres.
 */
@WebMvcTest(controllers = BookInterestController.class)
@Import({I18nConfig.class, MessageResolver.class})
class BookInterestControllerTest {

    /**
     * Um slice de {@code @WebMvcTest} nao carrega o {@code SecurityConfig}, e com
     * ele fica de fora o {@code @EnableMethodSecurity} — sem esta configuracao os
     * {@code @PreAuthorize} do controller nao rodam e todo teste de papel passaria
     * por acidente, com qualquer papel.
     */
    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityForTheSlice {
    }

    private static final UUID BOOK_ID = UUID.fromString("00000000-0000-4000-8000-000000003086");
    private static final OffsetDateTime MARKED_AT = OffsetDateTime.parse("2026-03-01T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookInterestService bookInterestService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(roles = "READER")
    void markingReturnsTheStateAndNotAMessage() throws Exception {
        when(bookInterestService.marcar(BOOK_ID))
                .thenReturn(BookInterestStateResponse.marked(BOOK_ID, MARKED_AT));

        mockMvc.perform(post("/api/books/{id}/interest", BOOK_ID).with(csrf()).header("Accept-Language", "pt-BR"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Language", "pt-BR"))
                .andExpect(jsonPath("$.bookId").value(BOOK_ID.toString()))
                .andExpect(jsonPath("$.interested").value(true))
                .andExpect(jsonPath("$.markedAt").exists());
    }

    /**
     * Marcar duas vezes responde 200 nas duas, com o mesmo corpo — nunca 409.
     * Duplo toque em botao de coracao e rotina em tela de celular, e obrigar o
     * cliente a tratar isso como erro seria transformar rotina em excecao.
     */
    @Test
    @WithMockUser(roles = "READER")
    void markingTwiceAnswersTheSameThingTwice() throws Exception {
        when(bookInterestService.marcar(BOOK_ID))
                .thenReturn(BookInterestStateResponse.marked(BOOK_ID, MARKED_AT));

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/books/{id}/interest", BOOK_ID).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.interested").value(true))
                    .andExpect(jsonPath("$.markedAt").value("2026-03-01T10:00:00Z"));
        }
    }

    @Test
    @WithMockUser(roles = "READER")
    void removingAnswersWithABodySoTheClientParsesItTheSameWay() throws Exception {
        when(bookInterestService.desmarcar(BOOK_ID)).thenReturn(BookInterestStateResponse.cleared(BOOK_ID));

        mockMvc.perform(delete("/api/books/{id}/interest", BOOK_ID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interested").value(false))
                .andExpect(jsonPath("$.markedAt").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "READER")
    void theOwnListCarriesTheCardAndTheMarkDate() throws Exception {
        BookCardResponse card = BookCardResponse.builder()
                .id(BOOK_ID)
                .title("Dom Casmurro")
                .author("Machado de Assis")
                .coverUrl("http://localhost/covers/dom-casmurro.jpg")
                .rating(4.8)
                .updatedAt(OffsetDateTime.parse("2026-02-01T08:00:00Z"))
                .build();
        when(bookInterestService.listarDoLeitorAutenticado(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new BookInterestResponse(card, MARKED_AT))));

        mockMvc.perform(get("/api/books/interests/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].book.id").value(BOOK_ID.toString()))
                .andExpect(jsonPath("$.content[0].book.title").value("Dom Casmurro"))
                .andExpect(jsonPath("$.content[0].book.updatedAt").exists())
                .andExpect(jsonPath("$.content[0].markedAt").value("2026-03-01T10:00:00Z"));
    }

    /**
     * O corpo do resumo e a decisao de privacidade em forma de contrato: livro,
     * quantos querem, quantos exemplares. Nenhum campo de leitor, em nenhum
     * lugar. Se alguem acrescentar um, este teste quebra.
     */
    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void theSummaryIsAggregateAndNeverNominal() throws Exception {
        when(bookInterestService.resumir(anyBoolean(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new BookInterestSummaryResponse(
                        BOOK_ID, "Dom Casmurro", "Machado de Assis", null,
                        OffsetDateTime.parse("2026-02-01T08:00:00Z"), 18, 1, 0))));

        String body = mockMvc.perform(get("/api/books/interests/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].interestCount").value(18))
                .andExpect(jsonPath("$.content[0].totalCopies").value(1))
                .andExpect(jsonPath("$.content[0].availableCopies").value(0))
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("reader", "registrationNumber", "matricula", "fullName");
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void theSummaryCanBeNarrowedToWhatTheLibraryCannotServe() throws Exception {
        when(bookInterestService.resumir(anyBoolean(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/books/interests/summary").param("unmetOnly", "true"))
                .andExpect(status().isOk());

        verify(bookInterestService).resumir(org.mockito.ArgumentMatchers.eq(true), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void staffCannotMarkInterestOnBehalfOfAnyone() throws Exception {
        mockMvc.perform(post("/api/books/{id}/interest", BOOK_ID).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/books/{id}/interest", BOOK_ID).with(csrf()))
                .andExpect(status().isForbidden());

        verify(bookInterestService, never()).marcar(any());
        verify(bookInterestService, never()).desmarcar(any());
    }

    /**
     * A lista de interesses e do proprio leitor e de mais ninguem: nao existe
     * versao nominal nem para a equipe.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void staffCannotReadAReadersInterestList() throws Exception {
        mockMvc.perform(get("/api/books/interests/mine"))
                .andExpect(status().isForbidden());

        verify(bookInterestService, never()).listarDoLeitorAutenticado(any());
    }

    @Test
    @WithMockUser(roles = "READER")
    void readerCannotReadTheLibraryIndicator() throws Exception {
        mockMvc.perform(get("/api/books/interests/summary"))
                .andExpect(status().isForbidden());

        verify(bookInterestService, never()).resumir(anyBoolean(), any());
    }

    @Test
    @WithMockUser(roles = "READER")
    void aReaderAccountWithoutAReaderGetsALocalizedFourHundred() throws Exception {
        when(bookInterestService.marcar(BOOK_ID))
                .thenThrow(BusinessRuleException.ofKey("interest.reader-required"));

        mockMvc.perform(post("/api/books/{id}/interest", BOOK_ID).with(csrf()).header("Accept-Language", "en-US"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Language", "en-US"))
                .andExpect(jsonPath("$.message").value("Only a reader can express interest in a book."));
    }

    @Test
    @WithMockUser(roles = "READER")
    void anInvalidBookIdIsFourHundredAndNotFiveHundred() throws Exception {
        mockMvc.perform(post("/api/books/{id}/interest", "nao-e-uuid").with(csrf()).header("Accept-Language", "pt-BR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations.id").value("Valor inválido para este parâmetro."));
    }
}
