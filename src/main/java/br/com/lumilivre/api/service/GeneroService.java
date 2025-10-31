package br.com.lumilivre.api.service;

import br.com.lumilivre.api.model.GeneroModel;
import br.com.lumilivre.api.repository.GeneroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class GeneroService {

    @Autowired
    private GeneroRepository generoRepository;

    public Set<GeneroModel> sugerirGenerosPorCdd(String cddCodigo) {
        return generoRepository.findAllByCddCodigo(cddCodigo);
    }
}