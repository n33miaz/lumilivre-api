package br.com.lumilivre.api.service.infra.bookmetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import br.com.lumilivre.api.dto.integration.brasilapi.BrasilApiResponse;

@ExtendWith(MockitoExtension.class)
class BrasilApiProviderTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    void findByIsbnReturnsEmptyForBlankIsbn() {
        assertThat(provider().findByIsbn(" ")).isEmpty();
        assertThat(provider().findByIsbn(null)).isEmpty();

        verifyNoInteractions(restTemplate);
    }

    @Test
    void findByIsbnMapsResponseToMetadata() {
        BrasilApiResponse response = BrasilApiResponse.builder()
                .isbn("9788535914849")
                .title("Dom Casmurro")
                .authors(List.of("Machado de Assis"))
                .publisher("Companhia das Letras")
                .synopsis("Romance classico brasileiro.")
                .pageCount(256)
                .year(1899)
                .coverUrl("https://cover.example/dom.jpg")
                .build();
        when(restTemplate.getForObject(anyString(), eq(BrasilApiResponse.class))).thenReturn(response);

        BookMetadata metadata = provider().findByIsbn("978-85-359-1484-9").orElseThrow();

        assertThat(metadata.providerName()).isEqualTo("brasilApi");
        assertThat(metadata.isbn()).isEqualTo("9788535914849");
        assertThat(metadata.title()).isEqualTo("Dom Casmurro");
        assertThat(metadata.author()).isEqualTo("Machado de Assis");
        assertThat(metadata.publisher()).isEqualTo("Companhia das Letras");
        assertThat(metadata.synopsis()).contains("Romance");
        assertThat(metadata.pageCount()).isEqualTo(256);
        assertThat(metadata.publicationDate()).isEqualTo(LocalDate.of(1899, 1, 1));
        assertThat(metadata.coverUrl()).isEqualTo("https://cover.example/dom.jpg");
    }

    @Test
    void findByIsbnJoinsMultipleAuthors() {
        BrasilApiResponse response = BrasilApiResponse.builder()
                .isbn("9780132350884")
                .title("Clean Code")
                .authors(List.of("Robert C. Martin", "Martin Fowler"))
                .build();
        when(restTemplate.getForObject(anyString(), eq(BrasilApiResponse.class))).thenReturn(response);

        BookMetadata metadata = provider().findByIsbn("9780132350884").orElseThrow();

        assertThat(metadata.author()).isEqualTo("Robert C. Martin, Martin Fowler");
        assertThat(metadata.publicationDate()).isNull();
    }

    @Test
    void findByIsbnReturnsEmptyWhenResponseIsNull() {
        when(restTemplate.getForObject(anyString(), eq(BrasilApiResponse.class))).thenReturn(null);

        assertThat(provider().findByIsbn("9780132350884")).isEmpty();
    }

    @Test
    void findByIsbnSwallowsNotFoundFromBrasilApi() {
        when(restTemplate.getForObject(anyString(), eq(BrasilApiResponse.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        assertThat(provider().findByIsbn("0000000000")).isEmpty();
    }

    @Test
    void nameIsBrasilApi() {
        assertThat(provider().name()).isEqualTo("brasilApi");
    }

    private BrasilApiProvider provider() {
        return new BrasilApiProvider(restTemplate);
    }
}
