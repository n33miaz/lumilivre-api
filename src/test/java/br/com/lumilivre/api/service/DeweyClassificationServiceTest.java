package br.com.lumilivre.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import br.com.lumilivre.api.model.DeweyClassification;
import br.com.lumilivre.api.repository.DeweyClassificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeweyClassificationServiceTest {

    @Mock
    private DeweyClassificationRepository deweyClassificationRepository;

    @Test
    void listMapsClassificationsToPublicResponses() {
        when(deweyClassificationRepository.findAll()).thenReturn(List.of(
                new DeweyClassification("000", "Computer science, information and general works"),
                new DeweyClassification("800", "Literature")));

        var result = new DeweyClassificationService(deweyClassificationRepository).list();

        assertThat(result).extracting("code").containsExactly("000", "800");
        assertThat(result).extracting("description")
                .containsExactly("Computer science, information and general works", "Literature");
    }

    @Test
    void listReturnsEmptyResponseWhenRepositoryIsEmpty() {
        when(deweyClassificationRepository.findAll()).thenReturn(List.of());

        assertThat(new DeweyClassificationService(deweyClassificationRepository).list()).isEmpty();
    }
}
