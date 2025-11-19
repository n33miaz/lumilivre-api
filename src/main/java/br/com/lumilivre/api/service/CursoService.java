package br.com.lumilivre.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.dto.requests.CursoRequestDTO;
import br.com.lumilivre.api.dto.responses.CursoResponseDTO;
import br.com.lumilivre.api.dto.ListaCursoDTO;
import br.com.lumilivre.api.exception.custom.RecursoNaoEncontradoException;
import br.com.lumilivre.api.model.CursoModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.CursoRepository;

@Service
public class CursoService {

    @Autowired
    private CursoRepository cr;

    public Page<ListaCursoDTO> buscarCursoParaListaAdmin(String texto, Pageable pageable) {
        return cr.findCursoParaListaAdminComFiltro(texto, pageable);
    }

    public Page<ListaCursoDTO> buscarPorTexto(String texto, Pageable pageable) {
        return cr.buscarPorTextoComDTO(texto, pageable);
    }

    public Page<ListaCursoDTO> buscarAvancado(String nome, Pageable pageable) {
        String nomeFiltro = (nome != null && !nome.isBlank()) ? "%" + nome + "%" : null;
        return cr.buscarAvancadoComDTO(nomeFiltro, pageable);
    }

    @Transactional
    public ResponseEntity<CursoResponseDTO> cadastrar(CursoRequestDTO dto) {
        CursoModel curso = new CursoModel();
        curso.setNome(dto.getNome());

        CursoModel salvo = cr.save(curso);

        return ResponseEntity.status(HttpStatus.CREATED).body(new CursoResponseDTO(salvo));
    }

    @Transactional
    public ResponseEntity<CursoResponseDTO> atualizar(Integer id, CursoRequestDTO dto) {
        CursoModel curso = cr.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso não encontrado com ID: " + id));

        curso.setNome(dto.getNome());

        CursoModel salvo = cr.save(curso);

        return ResponseEntity.ok(new CursoResponseDTO(salvo));
    }

    @Transactional
    public ResponseEntity<ResponseModel> excluir(Integer id) {
        if (!cr.existsById(id)) {
            throw new RecursoNaoEncontradoException("Curso não encontrado com ID: " + id);
        }

        cr.deleteById(id);

        ResponseModel rm = new ResponseModel();
        rm.setMensagem("O Curso foi removido com sucesso");
        return new ResponseEntity<>(rm, HttpStatus.OK);
    }
}