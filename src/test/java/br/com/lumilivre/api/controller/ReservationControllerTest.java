package br.com.lumilivre.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.config.MethodSecuritySliceConfig;
import br.com.lumilivre.api.mapper.ReservationMapper;
import br.com.lumilivre.api.model.Reservation;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.security.ReaderAuthorizationService;
import br.com.lumilivre.api.service.EnumLabelResolver;
import br.com.lumilivre.api.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReservationController.class)
@Import({MethodSecuritySliceConfig.class, I18nConfig.class, MessageResolver.class, ReservationMapper.class, EnumLabelResolver.class})
@WithMockUser(roles = "READER")
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean(name = "readerAuthz")
    private ReaderAuthorizationService readerAuthorizationService;

    @Test
    void createReturnsPtBRContentLanguage() throws Exception {
        Reservation reservation = new Reservation();
        when(readerAuthorizationService.canAccess("12345")).thenReturn(true);
        when(reservationService.criarReserva(anyString(), any(UUID.class))).thenReturn(reservation);

        mockMvc.perform(post("/api/reservations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"readerRegistrationNumber\":\"12345\",\"bookId\":\"" + UUID.randomUUID() + "\"}")
                        .header("Accept-Language", "pt-BR"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Content-Language", "pt-BR"));
    }

    @Test
    void createReturnsEnUSContentLanguage() throws Exception {
        Reservation reservation = new Reservation();
        when(readerAuthorizationService.canAccess("12345")).thenReturn(true);
        when(reservationService.criarReserva(anyString(), any(UUID.class))).thenReturn(reservation);

        mockMvc.perform(post("/api/reservations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"readerRegistrationNumber\":\"12345\",\"bookId\":\"" + UUID.randomUUID() + "\"}")
                        .header("Accept-Language", "en-US"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Content-Language", "en-US"));
    }

    /**
     * A matrícula da reserva vem do <b>corpo</b>, não do token: sem o
     * {@code @readerAuthz.canAccess(#request.readerRegistrationNumber)} bastaria
     * trocar uma string no JSON para pôr o colega na fila de um livro — ou para
     * consumir a cota de reservas dele.
     */
    @Test
    void aReaderCannotReserveInSomeoneElsesName() throws Exception {
        when(readerAuthorizationService.canAccess("99999")).thenReturn(false);

        mockMvc.perform(post("/api/reservations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"readerRegistrationNumber\":\"99999\",\"bookId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden());

        verify(reservationService, never()).criarReserva(anyString(), any(UUID.class));
    }

    /**
     * Mesma regra no cancelamento: sem o guarda, derrubar a reserva de um colega
     * seria um DELETE com a matrícula dele no query string.
     */
    @Test
    void aReaderCannotCancelSomeoneElsesReservation() throws Exception {
        when(readerAuthorizationService.canAccess("99999")).thenReturn(false);

        mockMvc.perform(delete("/api/reservations/{id}/cancel", UUID.randomUUID()).with(csrf())
                        .param("readerRegistrationNumber", "99999"))
                .andExpect(status().isForbidden());

        verify(reservationService, never()).cancelarReserva(any(UUID.class), anyString());
    }
}
