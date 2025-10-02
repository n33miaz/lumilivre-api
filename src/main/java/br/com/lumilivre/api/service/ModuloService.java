package br.com.lumilivre.api.service;

import br.com.lumilivre.api.model.ModuloModel;
import br.com.lumilivre.api.repository.ModuloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ModuloService {

    @Autowired
    private ModuloRepository moduloRepository;

    public List<ModuloModel> listarTodos() {
        return moduloRepository.findAllByOrderByNomeAsc();
    }

    public ModuloModel cadastrar(ModuloModel modulo) {
        // valida se o nome ja existe
        return moduloRepository.save(modulo);
    }
}