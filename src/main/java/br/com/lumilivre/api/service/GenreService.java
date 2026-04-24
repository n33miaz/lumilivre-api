package br.com.lumilivre.api.service;

import br.com.lumilivre.api.dto.genero.GeneroResponse;
import br.com.lumilivre.api.repository.GenreRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository genreRepository;

    @Cacheable("generos-dto")
    public List<GeneroResponse> listarTodos() {
        return genreRepository.findAll()
                .stream()
                .map(GeneroResponse::new)
                .collect(Collectors.toList());
    }
}
