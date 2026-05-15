package br.com.lumilivre.api.service;

import br.com.lumilivre.api.dto.v1.modulo.ModuloResumoResponse;
import br.com.lumilivre.api.dto.v1.modulo.ModuloRequest;
import br.com.lumilivre.api.dto.v1.modulo.ModuloResponse;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.model.AcademicModule;
import br.com.lumilivre.api.dto.v1.comum.ApiResponse;
import br.com.lumilivre.api.repository.AcademicModuleRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AcademicModuleService {

    private final AcademicModuleRepository academicModuleRepository;

    public Page<ModuloResumoResponse> buscarPorTexto(String texto, Pageable pageable) {
        return academicModuleRepository.buscarPorTextoComDTO(texto, pageable);
    }

    @Transactional
    @CacheEvict(value = "modulos", allEntries = true)
    public ResponseEntity<ModuloResponse> cadastrar(ModuloRequest dto) {
        if (academicModuleRepository.existsByNameIgnoreCase(dto.getNome())) {
            throw new BusinessRuleException("Já existe um módulo com este nome.");
        }
        AcademicModule modulo = new AcademicModule();
        modulo.setName(dto.getNome());
        AcademicModule salvo = academicModuleRepository.save(modulo);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ModuloResponse(salvo));
    }

    @Transactional
    @CacheEvict(value = "modulos", allEntries = true)
    public ResponseEntity<ModuloResponse> atualizar(Integer id, ModuloRequest dto) {
        AcademicModule modulo = academicModuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Módulo não encontrado."));

        modulo.setName(dto.getNome());
        AcademicModule salvo = academicModuleRepository.save(modulo);
        return ResponseEntity.ok(new ModuloResponse(salvo));
    }

    @Transactional
    @CacheEvict(value = "modulos", allEntries = true)
    public ResponseEntity<ApiResponse<Void>> excluir(Integer id) {
        if (!academicModuleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Módulo não encontrado.");
        }
        academicModuleRepository.deleteById(id);

        return ResponseEntity.ok(new ApiResponse<>(true, "Módulo removido com sucesso.", null));
    }

    public java.util.List<br.com.lumilivre.api.dto.v1.comum.EstatisticaGraficoResponse> buscarTotalEmprestimosPorModulo() {
        return academicModuleRepository.findTotalEmprestimosPorModulo();
    }
}
