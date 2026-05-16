package br.com.lumilivre.api.service;

import br.com.lumilivre.api.dto.v1.turno.TurnoResumoResponse;
import br.com.lumilivre.api.dto.v1.turno.TurnoRequest;
import br.com.lumilivre.api.dto.v1.turno.TurnoResponse;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.model.StudyShift;
import br.com.lumilivre.api.dto.v1.comum.ApiResponse;
import br.com.lumilivre.api.repository.StudyShiftRepository;
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
public class StudyShiftService {

    private final StudyShiftRepository studyShiftRepository;

    public Page<TurnoResumoResponse> buscarPorTexto(String texto, Pageable pageable) {
        return studyShiftRepository.buscarPorTextoComDTO(texto, pageable);
    }

    @Transactional
    @CacheEvict(value = "turnos", allEntries = true)
    public ResponseEntity<TurnoResponse> cadastrar(TurnoRequest dto) {
        if (studyShiftRepository.existsByNameIgnoreCase(dto.getNome())) {
            throw BusinessRuleException.ofKey("metadata.study-shift.name.already-exists");
        }
        StudyShift turno = new StudyShift();
        turno.setName(dto.getNome());
        StudyShift salvo = studyShiftRepository.save(turno);
        return ResponseEntity.status(HttpStatus.CREATED).body(new TurnoResponse(salvo));
    }

    @Transactional
    @CacheEvict(value = "turnos", allEntries = true)
    public ResponseEntity<TurnoResponse> atualizar(Integer id, TurnoRequest dto) {
        StudyShift turno = studyShiftRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("metadata.study-shift.not-found"));

        turno.setName(dto.getNome());
        StudyShift salvo = studyShiftRepository.save(turno);
        return ResponseEntity.ok(new TurnoResponse(salvo));
    }

    @Transactional
    @CacheEvict(value = "turnos", allEntries = true)
    public ResponseEntity<ApiResponse<Void>> excluir(Integer id) {
        if (!studyShiftRepository.existsById(id)) {
            throw ResourceNotFoundException.ofKey("metadata.study-shift.not-found");
        }
        studyShiftRepository.deleteById(id);

        return ResponseEntity.ok(new ApiResponse<>(true, "Turno removido com sucesso.", null));
    }

    public java.util.List<br.com.lumilivre.api.dto.v1.comum.EstatisticaGraficoResponse> buscarTotalEmprestimosPorTurno() {
        return studyShiftRepository.findTotalEmprestimosPorTurno();
    }
}
