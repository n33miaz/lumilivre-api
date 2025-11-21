package br.com.lumilivre.api.service;

import br.com.lumilivre.api.dto.modulo.ModuloResumoResponse;
import br.com.lumilivre.api.dto.modulo.ModuloRequest;
import br.com.lumilivre.api.dto.modulo.ModuloResponse;
import br.com.lumilivre.api.exception.custom.RecursoNaoEncontradoException;
import br.com.lumilivre.api.exception.custom.RegraDeNegocioException;
import br.com.lumilivre.api.model.ModuloModel;
import br.com.lumilivre.api.dto.comum.ApiResponse;
import br.com.lumilivre.api.repository.ModuloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModuloService {

    @Autowired
    private ModuloRepository moduloRepository;

    public Page<ModuloResumoResponse> buscarPorTexto(String texto, Pageable pageable) {
        return moduloRepository.buscarPorTextoComDTO(texto, pageable);
    }

    @Transactional
    @CacheEvict(value = "modulos", allEntries = true)
    public ResponseEntity<ModuloResponse> cadastrar(ModuloRequest dto) {
        if (moduloRepository.existsByNomeIgnoreCase(dto.getNome())) {
            throw new RegraDeNegocioException("Já existe um módulo com este nome.");
        }
        ModuloModel modulo = new ModuloModel();
        modulo.setNome(dto.getNome());
        ModuloModel salvo = moduloRepository.save(modulo);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ModuloResponse(salvo));
    }

    @Transactional
    @CacheEvict(value = "modulos", allEntries = true)
    public ResponseEntity<ModuloResponse> atualizar(Integer id, ModuloRequest dto) {
        ModuloModel modulo = moduloRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Módulo não encontrado."));

        modulo.setNome(dto.getNome());
        ModuloModel salvo = moduloRepository.save(modulo);
        return ResponseEntity.ok(new ModuloResponse(salvo));
    }

    @Transactional
    @CacheEvict(value = "modulos", allEntries = true)
    public ResponseEntity<ApiResponse<Void>> excluir(Integer id) {
        if (!moduloRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Módulo não encontrado.");
        }
        moduloRepository.deleteById(id);

        return ResponseEntity.ok(new ApiResponse<>(true, "Módulo removido com sucesso.", null));
    }
}