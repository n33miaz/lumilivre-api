package br.com.lumilivre.api.service.infra.bookmetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import br.com.lumilivre.api.dto.integration.google.GoogleBooksResponse;
import br.com.lumilivre.api.dto.integration.google.ImageLinks;
import br.com.lumilivre.api.dto.integration.google.VolumeInfo;
import br.com.lumilivre.api.dto.integration.google.VolumeItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class GoogleBooksProviderTest {

    @Mock
    private RestTemplate restTemplate;

    @Test
    void findByIsbnReturnsEmptyForBlankIsbn() {
        assertThat(provider().findByIsbn(" ")).isEmpty();

        verifyNoInteractions(restTemplate);
    }

    @Test
    void findByIsbnMapsFirstVolumeToMetadata() {
        when(restTemplate.getForObject(anyString(), eq(GoogleBooksResponse.class)))
                .thenReturn(new GoogleBooksResponse(List.of(new VolumeItem(volume()
                        .publishedDate("2024-09-15")
                        .imageLinks(ImageLinks.builder()
                                .thumbnail("http://cdn.test/thumb.jpg")
                                .build())
                        .build()))));

        BookMetadata metadata = provider().findByIsbn("9780132350884").orElseThrow();

        assertThat(metadata.providerName()).isEqualTo("googleBooks");
        assertThat(metadata.title()).isEqualTo("Clean Code");
        assertThat(metadata.author()).isEqualTo("Robert C. Martin, Martin Fowler");
        assertThat(metadata.publisher()).isEqualTo("Prentice Hall");
        assertThat(metadata.synopsis()).isEqualTo("Craftsmanship");
        assertThat(metadata.pageCount()).isEqualTo(464);
        assertThat(metadata.rating()).isEqualTo(4.8);
        assertThat(metadata.categories()).containsExactly("Software");
        assertThat(metadata.publicationDate()).isEqualTo(LocalDate.of(2024, 9, 15));
        assertThat(metadata.coverUrl()).isEqualTo("https://cdn.test/thumb.jpg");
    }

    @Test
    void findByTitleAndAuthorSupportsPartialDatesAndImagePriority() {
        when(restTemplate.getForObject(anyString(), eq(GoogleBooksResponse.class)))
                .thenReturn(new GoogleBooksResponse(List.of(new VolumeItem(volume()
                        .publishedDate("2024-09")
                        .imageLinks(ImageLinks.builder()
                                .smallThumbnail("http://cdn.test/small.jpg")
                                .large("https://cdn.test/large.jpg")
                                .build())
                        .build()))))
                .thenReturn(new GoogleBooksResponse(List.of(new VolumeItem(volume()
                        .publishedDate("2024")
                        .imageLinks(null)
                        .build()))));

        BookMetadata byMonth = provider().findByTitleAndAuthor("Clean Code", "Martin").orElseThrow();
        BookMetadata byYear = provider().findByTitleAndAuthor("Clean Code", null).orElseThrow();

        assertThat(byMonth.publicationDate()).isEqualTo(LocalDate.of(2024, 9, 1));
        assertThat(byMonth.coverUrl()).isEqualTo("https://cdn.test/large.jpg");
        assertThat(byYear.publicationDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(byYear.coverUrl()).isNull();
    }

    @Test
    void findByTitleAndAuthorReturnsEmptyForBlankTitle() {
        assertThat(provider().findByTitleAndAuthor(" ", "Author")).isEmpty();

        verifyNoInteractions(restTemplate);
    }

    @Test
    void queryReturnsEmptyForNullEmptyOrNullVolumeInfoResponses() {
        when(restTemplate.getForObject(anyString(), eq(GoogleBooksResponse.class)))
                .thenReturn(null)
                .thenReturn(new GoogleBooksResponse(List.of()))
                .thenReturn(new GoogleBooksResponse(List.of(new VolumeItem(null))));

        assertThat(provider().findByIsbn("111")).isEmpty();
        assertThat(provider().findByIsbn("222")).isEmpty();
        assertThat(provider().findByIsbn("333")).isEmpty();
    }

    private GoogleBooksProvider provider() {
        return new GoogleBooksProvider(restTemplate);
    }

    private static VolumeInfo.VolumeInfoBuilder volume() {
        return VolumeInfo.builder()
                .title("Clean Code")
                .authors(List.of("Robert C. Martin", "Martin Fowler"))
                .publisher("Prentice Hall")
                .description("Craftsmanship")
                .pageCount(464)
                .averageRating(4.8)
                .categories(List.of("Software"));
    }
}
