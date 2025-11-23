package br.com.lumilivre.api.service;

import br.com.lumilivre.api.dto.turno.TurnoResumoResponse;
import br.com.lumilivre.api.dto.turno.TurnoRequest;
import br.com.lumilivre.api.dto.turno.TurnoResponse;
import br.com.lumilivre.api.exception.custom.RecursoNaoEncontradoException;
import br.com.lumilivre.api.exception.custom.RegraDeNegocioException;
import br.com.lumilivre.api.model.TurnoModel;
import br.com.lumilivre.api.dto.comum.ApiResponse;
import br.com.lumilivre.api.repository.TurnoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TurnoService {

    @Autowired
    private TurnoRepository turnoRepository;

    public Page<TurnoResumoResponse> buscarPorTexto(String texto, Pageable pageable) {
        return turnoRepository.buscarPorTextoComDTO(texto, pageable);
    }

    @Transactional
    @CacheEvict(value = "turnos", allEntries = true)
    public ResponseEntity<TurnoResponse> cadastrar(TurnoRequest dto) {
        if (turnoRepository.existsByNomeIgnoreCase(dto.getNome())) {
            throw new RegraDeNegocioException("Já existe um turno com este nome.");
        }
        TurnoModel turno = new TurnoModel();
        turno.setNome(dto.getNome());
        TurnoModel salvo = turnoRepository.save(turno);
        return ResponseEntity.status(HttpStatus.CREATED).body(new TurnoResponse(salvo));
    }

    @Transactional
    @CacheEvict(value = "turnos", allEntries = true)
    public ResponseEntity<TurnoResponse> atualizar(Integer id, TurnoRequest dto) {
        TurnoModel turno = turnoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turno não encontrado."));

        turno.setNome(dto.getNome());
        TurnoModel salvo = turnoRepository.save(turno);
        return ResponseEntity.ok(new TurnoResponse(salvo));
    }

    @Transactional
    @CacheEvict(value = "turnos", allEntries = true)
    public ResponseEntity<ApiResponse<Void>> excluir(Integer id) {
        if (!turnoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Turno não encontrado.");
        }
        turnoRepository.deleteById(id);

        return ResponseEntity.ok(new ApiResponse<>(true, "Turno removido com sucesso.", null));
    }

    public java.util.List<br.com.lumilivre.api.dto.comum.EstatisticaGraficoResponse> buscarTotalEmprestimosPorTurno() {
        return turnoRepository.findTotalEmprestimosPorTurno();
    }
}