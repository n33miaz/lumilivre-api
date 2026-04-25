package br.com.lumilivre.api.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.dto.curso.CursoRequest;
import br.com.lumilivre.api.dto.curso.CursoResponse;
import br.com.lumilivre.api.dto.curso.CursoResumoResponse;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.Course;
import br.com.lumilivre.api.dto.comum.ApiResponse;
import br.com.lumilivre.api.repository.CourseRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    public Page<CursoResumoResponse> buscarCursoParaListaAdmin(String texto, Pageable pageable) {
        return courseRepository.findCursoParaListaAdminComFiltro(texto, pageable);
    }

    public Page<CursoResumoResponse> buscarPorTexto(String texto, Pageable pageable) {
        String textoFormatado = (texto != null && !texto.isBlank()) ? "%" + texto + "%" : null;
        return courseRepository.buscarPorTextoComDTO(textoFormatado, pageable);
    }

    public Page<CursoResumoResponse> buscarAvancado(String nome, Pageable pageable) {
        String nomeFiltro = (nome != null && !nome.isBlank()) ? "%" + nome + "%" : null;
        return courseRepository.buscarAvancadoComDTO(nomeFiltro, pageable);
    }

    @Transactional
    public ResponseEntity<CursoResponse> cadastrar(CursoRequest dto) {
        Course curso = new Course();
        curso.setName(dto.getNome());

        Course salvo = courseRepository.save(curso);

        return ResponseEntity.status(HttpStatus.CREATED).body(new CursoResponse(salvo));
    }

    @Transactional
    public ResponseEntity<CursoResponse> atualizar(Integer id, CursoRequest dto) {
        Course curso = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Curso não encontrado com ID: " + id));

        curso.setName(dto.getNome());

        Course salvo = courseRepository.save(curso);

        return ResponseEntity.ok(new CursoResponse(salvo));
    }

    @Transactional
    public ResponseEntity<ApiResponse<Void>> excluir(Integer id) {
        if (!courseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Curso não encontrado com ID: " + id);
        }

        courseRepository.deleteById(id);

        return ResponseEntity.ok(new ApiResponse<>(true, "O Curso foi removido com sucesso", null));
    }

    public List<br.com.lumilivre.api.dto.curso.CursoEstatisticaResponse> buscarEstatisticas() {
        return courseRepository.findEstatisticasCursos();
    }

    public List<br.com.lumilivre.api.dto.comum.EstatisticaGraficoResponse> buscarTotalEmprestimosPorCurso() {
        return courseRepository.findTotalEmprestimosPorCurso();
    }
}
