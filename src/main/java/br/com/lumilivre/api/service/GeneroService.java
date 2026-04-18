package br.com.lumilivre.api.service;

import br.com.lumilivre.api.dto.genero.GeneroResponse;
import br.com.lumilivre.api.repository.GeneroRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GeneroService {

    private final GeneroRepository generoRepository;

    @Cacheable("generos-dto")
    public List<GeneroResponse> listarTodos() {
        return generoRepository.findAll()
                .stream()
                .map(GeneroResponse::new)
                .collect(Collectors.toList());
    }
}
