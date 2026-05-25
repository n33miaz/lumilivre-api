package br.com.lumilivre.api.service;

import java.util.List;

import org.springframework.stereotype.Service;

import br.com.lumilivre.api.dto.genre.GenreResponse;
import br.com.lumilivre.api.repository.GenreRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository genreRepository;

    public List<GenreResponse> list() {
        return genreRepository.findAll()
                .stream()
                .map(genre -> new GenreResponse(genre.getId(), genre.getName()))
                .toList();
    }
}
