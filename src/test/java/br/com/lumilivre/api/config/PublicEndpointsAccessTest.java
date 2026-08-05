package br.com.lumilivre.api.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import br.com.lumilivre.api.dto.book.BookCopyCounts;
import br.com.lumilivre.api.dto.settings.SettingsFeaturesResponse;
import br.com.lumilivre.api.dto.settings.SettingsPublicResponse;
import br.com.lumilivre.api.enums.LibraryType;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.service.AccessLogService;
import br.com.lumilivre.api.service.BookService;
import br.com.lumilivre.api.service.SettingsService;

/**
 * O acesso de convidado depende de duas rotas anônimas, e abrir rota é decisão de
 * segurança. Este teste sobe o {@code SecurityConfig} de verdade (as regras de
 * URL não existem num slice de {@code @WebMvcTest}) e trava as duas pontas:
 * o convidado consegue chegar, e o corpo que ele recebe não tem campo a mais.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "app.scheduling.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:lumilivre_public_access;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "jwt.secret=test-secret-key-with-enough-length-for-hmac-signature",
        "supabase.url=https://example.supabase.co",
        "supabase.key=test-key",
        "supabase.service-role.key=test-service-role-key",
        "app.cors.allowed-origins=http://localhost:5173"
})
@AutoConfigureMockMvc
@WithAnonymousUser
class PublicEndpointsAccessTest {

    private static final UUID BOOK_ID = UUID.fromString("00000000-0000-4000-8000-000000003086");

    /**
     * Campos que {@link br.com.lumilivre.api.dto.book.BookResponse} pode expor a
     * um anônimo: dado bibliográfico — o conteúdo da ficha catalográfica — mais
     * duas contagens de exemplares e o carimbo de atualização. Se alguém
     * acrescentar dado de pessoa, de empréstimo ou de exemplar <i>identificado</i>
     * ao DTO, este teste quebra, e é exatamente essa a intenção.
     *
     * <p>Por que a disponibilidade entrou depois de a rota virar pública: o T04
     * fechou o DTO para "nada de exemplar" pensando em tombo, prateleira e status
     * de exemplar específico — o que ajuda alguém a localizar fisicamente um
     * livro. Contagem não é nada disso. "3 de 5 disponíveis" é o que qualquer
     * OPAC de biblioteca pública mostra na estante virtual, e a sua ausência
     * tinha um custo concreto: o app lia campo ausente como zero e o botão de
     * solicitar empréstimo ficava morto em todo livro, para todo leitor. O que
     * continua fora está travado em {@link #publicBookRecordNeverIdentifiesACopy}.
     */
    private static final Set<String> ALLOWED_BOOK_FIELDS = Set.of(
            "id", "isbn", "title", "author", "publisher", "publicationDate", "pageCount",
            "synopsis", "coverUrl", "deweyCode", "ageRating", "coverType", "edition",
            "volume", "rating", "genres", "updatedAt", "totalCopies", "availableCopies");

    /**
     * Dado de exemplar que jamais pode aparecer na ficha pública. Separado da
     * allowlist porque a lista acima cresce quando alguém acrescenta um campo
     * legítimo; esta não cresce nunca.
     */
    private static final Set<String> FORBIDDEN_BOOK_FIELDS = Set.of(
            "copyCode", "copies", "shelfLocation", "physicalLocation", "copyStatus",
            "loans", "readers", "interestCount");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @MockBean
    private SettingsService settingsService;

    @MockBean
    private AccessLogService accessLogService;

    @Test
    void guestCanOpenABookRecord() throws Exception {
        // Era o único ponto do catálogo que exigia papel: o convidado listava os
        // livros e tomava 401 ao tocar em um deles, o que o app mostrava como
        // erro de rede.
        Mockito.when(bookService.findById(BOOK_ID)).thenReturn(Optional.of(book()));

        mockMvc.perform(get("/api/books/{id}", BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Dom Casmurro"))
                .andExpect(jsonPath("$.isbn").value("9788535910663"));
    }

    @Test
    void publicBookRecordCarriesOnlyBibliographicFields() throws Exception {
        Mockito.when(bookService.findById(BOOK_ID)).thenReturn(Optional.of(book()));

        String body = mockMvc.perform(get("/api/books/{id}", BOOK_ID))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> returned = new ArrayList<>();
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).fieldNames()
                .forEachRemaining(returned::add);
        org.assertj.core.api.Assertions.assertThat(returned).isSubsetOf(ALLOWED_BOOK_FIELDS);
    }

    @Test
    void publicBookRecordNeverIdentifiesACopy() throws Exception {
        // A ficha diz quantos exemplares existem e quantos estão livres. Não diz
        // qual exemplar, nem onde ele está, nem quem está com ele.
        Mockito.when(bookService.findById(BOOK_ID)).thenReturn(Optional.of(book()));
        Mockito.when(bookService.contarExemplares(BOOK_ID)).thenReturn(new BookCopyCounts(5, 3));

        String body = mockMvc.perform(get("/api/books/{id}", BOOK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCopies").value(5))
                .andExpect(jsonPath("$.availableCopies").value(3))
                .andReturn().getResponse().getContentAsString();

        List<String> returned = new ArrayList<>();
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(body).fieldNames()
                .forEachRemaining(returned::add);
        org.assertj.core.api.Assertions.assertThat(returned).doesNotContainAnyElementsOf(FORBIDDEN_BOOK_FIELDS);
    }

    @Test
    void guestCannotMarkInterest() throws Exception {
        // A ficha do livro é pública; o interesse não. Interesse sem dono
        // identificado não é dado, e o dono vem do token.
        mockMvc.perform(post("/api/books/{id}/interest", BOOK_ID))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/books/{id}/interest", BOOK_ID))
                .andExpect(status().isUnauthorized());
        refused(get("/api/books/interests/mine"));
        refused(get("/api/books/interests/summary"));
    }

    @Test
    void guestLearnsWhetherGuestAccessIsEnabled() throws Exception {
        // Sem esta rota o convidado nunca descobre que o modo convidado foi
        // desligado — pediria o catálogo e leria o erro como falha de rede.
        Mockito.when(settingsService.getPublicSettingsView()).thenReturn(new SettingsPublicResponse(
                LibraryType.SCHOOL, false, new SettingsFeaturesResponse(true, true, true)));

        mockMvc.perform(get("/api/settings/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestAccessEnabled").value(false))
                .andExpect(jsonPath("$.libraryType").value("SCHOOL"))
                .andExpect(jsonPath("$.features.ranking").value(true))
                // Permissão de leitor autenticado: não tem uso antes do login e
                // por isso não está na projeção pública.
                .andExpect(jsonPath("$.readerCanEditAvatar").doesNotExist());
    }

    @Test
    void fullSettingsStayBehindAuthentication() throws Exception {
        // O recorte público existe justamente para que o objeto inteiro continue
        // fechado: library_settings vai receber flags novas, e elas não podem
        // virar públicas só por causa da forma do endpoint.
        mockMvc.perform(get("/api/settings")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminListingsStayBehindAuthentication() throws Exception {
        // Abrir /api/books/{id} não pode ter aberto a família /api/books.
        // 401 ou 403 conforme a regra que barra (entry point x access denied
        // handler); o que importa é que o anônimo não lê.
        refused(get("/api/books"));
        refused(get("/api/books/search").param("q", "x"));
        refused(get("/api/books/grouped"));
        refused(get("/api/access-logs"));
    }

    private void refused(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        int status = mockMvc.perform(request).andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(status)
                .as("anonymous access must be refused")
                .isIn(401, 403);
    }

    private static Book book() {
        Book book = new Book();
        book.setId(BOOK_ID);
        book.setIsbn("9788535910663");
        book.setTitle("Dom Casmurro");
        book.setAuthor("Machado de Assis");
        book.setPublisher("Companhia das Letras");
        book.setPublicationDate(LocalDate.of(1899, 1, 1));
        book.setPageCount(256);
        book.setSynopsis("Bentinho e Capitu.");
        return book;
    }
}
