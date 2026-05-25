package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import br.com.lumilivre.api.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class MetadataServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Test
    void authorsFiltersCaseInsensitivelyAndReturnsRequestedPage() {
        when(bookRepository.countByAutor()).thenReturn(List.of(
                Map.of("autor", "Machado de Assis", "total", 3L),
                Map.of("autor", "Clarice Lispector", "total", 2L),
                Map.of("autor", "Machado Xavier", "total", 1L)));

        var page = service().authors("machado", PageRequest.of(0, 1));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting("name")
                .containsExactly("Machado de Assis");
    }

    @Test
    void authorsTreatsMissingTotalsAsZero() {
        when(bookRepository.countByAutor()).thenReturn(List.of(Map.of("autor", "Unknown")));

        var page = service().authors(null, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).totalBooks()).isZero();
    }

    private MetadataService service() {
        return new MetadataService(bookRepository);
    }
}
