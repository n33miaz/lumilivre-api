package br.com.lumilivre.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import br.com.lumilivre.api.config.I18nConfig;
import br.com.lumilivre.api.config.MessageResolver;
import br.com.lumilivre.api.mapper.ReservationMapper;
import br.com.lumilivre.api.model.Reservation;
import br.com.lumilivre.api.security.CustomUserDetailsService;
import br.com.lumilivre.api.security.JwtUtil;
import br.com.lumilivre.api.security.StudentAuthorizationService;
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
@Import({I18nConfig.class, MessageResolver.class, ReservationMapper.class, EnumLabelResolver.class})
@WithMockUser(roles = "STUDENT")
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private StudentAuthorizationService studentAuthorizationService;

    @Test
    void createReturnsPtBRContentLanguage() throws Exception {
        Reservation reservation = new Reservation();
        when(reservationService.criarReserva(anyString(), any(UUID.class))).thenReturn(reservation);

        mockMvc.perform(post("/api/reservations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentRegistrationNumber\":\"12345\",\"bookId\":\"" + UUID.randomUUID() + "\"}")
                        .header("Accept-Language", "pt-BR"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Content-Language", "pt-BR"));
    }

    @Test
    void createReturnsEnUSContentLanguage() throws Exception {
        Reservation reservation = new Reservation();
        when(reservationService.criarReserva(anyString(), any(UUID.class))).thenReturn(reservation);

        mockMvc.perform(post("/api/reservations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentRegistrationNumber\":\"12345\",\"bookId\":\"" + UUID.randomUUID() + "\"}")
                        .header("Accept-Language", "en-US"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Content-Language", "en-US"));
    }
}
