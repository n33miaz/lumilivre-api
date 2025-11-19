package br.com.lumilivre.api.service;

import br.com.lumilivre.api.dto.ListaModuloDTO;
import br.com.lumilivre.api.dto.requests.ModuloRequestDTO;
import br.com.lumilivre.api.dto.responses.ModuloResponseDTO;
import br.com.lumilivre.api.exception.custom.RecursoNaoEncontradoException;
import br.com.lumilivre.api.exception.custom.RegraDeNegocioException;
import br.com.lumilivre.api.model.ModuloModel;
import br.com.lumilivre.api.model.ResponseModel;
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

    public Page<ListaModuloDTO> buscarPorTexto(String texto, Pageable pageable) {
        return moduloRepository.buscarPorTextoComDTO(texto, pageable);
    }

    @Transactional
    @CacheEvict(value = "modulos", allEntries = true)
    public ResponseEntity<ModuloResponseDTO> cadastrar(ModuloRequestDTO dto) {
        if (moduloRepository.existsByNomeIgnoreCase(dto.getNome())) {
            throw new RegraDeNegocioException("Já existe um módulo com este nome.");
        }
        ModuloModel modulo = new ModuloModel();
        modulo.setNome(dto.getNome());
        ModuloModel salvo = moduloRepository.save(modulo);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ModuloResponseDTO(salvo));
    }

    @Transactional
    @CacheEvict(value = "modulos", allEntries = true)
    public ResponseEntity<ModuloResponseDTO> atualizar(Integer id, ModuloRequestDTO dto) {
        ModuloModel modulo = moduloRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Módulo não encontrado."));

        modulo.setNome(dto.getNome());
        ModuloModel salvo = moduloRepository.save(modulo);
        return ResponseEntity.ok(new ModuloResponseDTO(salvo));
    }

    @Transactional
    @CacheEvict(value = "modulos", allEntries = true)
    public ResponseEntity<ResponseModel> excluir(Integer id) {
        if (!moduloRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Módulo não encontrado.");
        }
        moduloRepository.deleteById(id);
        return ResponseEntity.ok(new ResponseModel("Módulo removido com sucesso."));
    }
}