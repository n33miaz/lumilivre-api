package br.com.lumilivre.api.service;

import br.com.lumilivre.api.dto.genero.GeneroResponse;
import br.com.lumilivre.api.repository.GeneroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GeneroService {

    @Autowired
    private GeneroRepository generoRepository;

    @Cacheable("generos-dto")
    public List<GeneroResponse> listarTodos() {
        return generoRepository.findAll()
                .stream()
                .map(GeneroResponse::new)
                .collect(Collectors.toList());
    }

    public Set<GeneroResponse> sugerirGenerosPorCdd(String cddCodigo) {
        return generoRepository.findAllByCddCodigo(cddCodigo)
                .stream()
                .map(GeneroResponse::new)
                .collect(Collectors.toSet());
    }
}