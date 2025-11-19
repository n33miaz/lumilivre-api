package br.com.lumilivre.api.service;

import br.com.lumilivre.api.dto.GeneroDTO;
import br.com.lumilivre.api.repository.GeneroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GeneroService {

    @Autowired
    private GeneroRepository generoRepository;

    public List<GeneroDTO> listarTodos() {
        return generoRepository.findAll()
                .stream()
                .map(GeneroDTO::new)
                .collect(Collectors.toList());
    }

    public Set<GeneroDTO> sugerirGenerosPorCdd(String cddCodigo) {
        return generoRepository.findAllByCddCodigo(cddCodigo)
                .stream()
                .map(GeneroDTO::new)
                .collect(Collectors.toSet());
    }
}