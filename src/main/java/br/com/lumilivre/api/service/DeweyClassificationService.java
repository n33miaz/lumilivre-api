package br.com.lumilivre.api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.lumilivre.api.dto.dewey.DeweyClassificationResponse;
import br.com.lumilivre.api.repository.DeweyClassificationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeweyClassificationService {

    private final DeweyClassificationRepository deweyClassificationRepository;

    public List<DeweyClassificationResponse> list() {
        return deweyClassificationRepository.findAll()
                .stream()
                .map(classification -> new DeweyClassificationResponse(
                        classification.getCode(),
                        classification.getDescription()))
                .toList();
    }
}
