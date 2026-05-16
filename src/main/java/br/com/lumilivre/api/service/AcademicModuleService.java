package br.com.lumilivre.api.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.dto.academicmodule.AcademicModuleRequest;
import br.com.lumilivre.api.dto.academicmodule.AcademicModuleSummaryResponse;
import br.com.lumilivre.api.dto.common.ChartItemResponse;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.AcademicModule;
import br.com.lumilivre.api.repository.AcademicModuleRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AcademicModuleService {

    private final AcademicModuleRepository academicModuleRepository;

    public Page<AcademicModuleSummaryResponse> buscarPorTexto(String texto, Pageable pageable) {
        return academicModuleRepository.findSummaries(texto, pageable);
    }

    @Transactional
    @CacheEvict(value = "modulos", allEntries = true)
    public AcademicModule cadastrar(AcademicModuleRequest request) {
        if (academicModuleRepository.existsByNameIgnoreCase(request.getName())) {
            throw BusinessRuleException.ofKey("metadata.academic-module.name.already-exists");
        }
        AcademicModule modulo = new AcademicModule();
        modulo.setName(request.getName());
        return academicModuleRepository.save(modulo);
    }

    @Transactional
    @CacheEvict(value = "modulos", allEntries = true)
    public AcademicModule atualizar(Integer id, AcademicModuleRequest request) {
        AcademicModule modulo = academicModuleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("metadata.academic-module.not-found"));

        modulo.setName(request.getName());
        return academicModuleRepository.save(modulo);
    }

    @Transactional
    @CacheEvict(value = "modulos", allEntries = true)
    public void excluir(Integer id) {
        if (!academicModuleRepository.existsById(id)) {
            throw ResourceNotFoundException.ofKey("metadata.academic-module.not-found");
        }
        academicModuleRepository.deleteById(id);
    }

    public List<ChartItemResponse> buscarTotalEmprestimosPorModulo() {
        return academicModuleRepository.findTotalEmprestimosPorModulo();
    }
}
