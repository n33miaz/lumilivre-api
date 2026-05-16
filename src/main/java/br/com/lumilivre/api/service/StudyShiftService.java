package br.com.lumilivre.api.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.dto.common.ChartItemResponse;
import br.com.lumilivre.api.dto.studyshift.StudyShiftRequest;
import br.com.lumilivre.api.dto.studyshift.StudyShiftSummaryResponse;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.StudyShift;
import br.com.lumilivre.api.repository.StudyShiftRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudyShiftService {

    private final StudyShiftRepository studyShiftRepository;

    public Page<StudyShiftSummaryResponse> buscarPorTexto(String texto, Pageable pageable) {
        return studyShiftRepository.findSummaries(texto, pageable);
    }

    @Transactional
    @CacheEvict(value = "turnos", allEntries = true)
    public StudyShift cadastrar(StudyShiftRequest request) {
        if (studyShiftRepository.existsByNameIgnoreCase(request.getName())) {
            throw BusinessRuleException.ofKey("metadata.study-shift.name.already-exists");
        }
        StudyShift turno = new StudyShift();
        turno.setName(request.getName());
        return studyShiftRepository.save(turno);
    }

    @Transactional
    @CacheEvict(value = "turnos", allEntries = true)
    public StudyShift atualizar(Integer id, StudyShiftRequest request) {
        StudyShift turno = studyShiftRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("metadata.study-shift.not-found"));

        turno.setName(request.getName());
        return studyShiftRepository.save(turno);
    }

    @Transactional
    @CacheEvict(value = "turnos", allEntries = true)
    public void excluir(Integer id) {
        if (!studyShiftRepository.existsById(id)) {
            throw ResourceNotFoundException.ofKey("metadata.study-shift.not-found");
        }
        studyShiftRepository.deleteById(id);
    }

    public List<ChartItemResponse> buscarTotalEmprestimosPorTurno() {
        return studyShiftRepository.findTotalEmprestimosPorTurno();
    }
}
