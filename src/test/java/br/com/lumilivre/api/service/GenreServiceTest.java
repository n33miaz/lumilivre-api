package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import br.com.lumilivre.api.model.Genre;
import br.com.lumilivre.api.repository.GenreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenreServiceTest {

    @Mock
    private GenreRepository genreRepository;

    @Test
    void listMapsGenresToPublicResponses() {
        when(genreRepository.findAll()).thenReturn(List.of(
                new Genre(1, "Romance"),
                new Genre(2, "Architecture")));

        var result = new GenreService(genreRepository).list();

        assertThat(result).extracting("id").containsExactly(1, 2);
        assertThat(result).extracting("name").containsExactly("Romance", "Architecture");
    }

    @Test
    void listReturnsEmptyResponseWhenRepositoryIsEmpty() {
        when(genreRepository.findAll()).thenReturn(List.of());

        assertThat(new GenreService(genreRepository).list()).isEmpty();
    }
}
