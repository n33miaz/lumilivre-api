package br.com.lumilivre.api.service;

import br.com.lumilivre.api.dto.comum.ApiResponse;
import br.com.lumilivre.api.dto.tcc.TccRequest;
import br.com.lumilivre.api.dto.tcc.TccResponse;
import br.com.lumilivre.api.model.CursoModel;
import br.com.lumilivre.api.model.TccModel;
import br.com.lumilivre.api.repository.CursoRepository;
import br.com.lumilivre.api.repository.TccRepository;
import br.com.lumilivre.api.service.infra.SupabaseStorageService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TccService {

    @Autowired
    private TccRepository tccRepository;

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    @Autowired
    private CursoRepository cursoRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    public ResponseEntity<ApiResponse<TccResponse>> cadastrarTcc(String dadosJson, MultipartFile arquivoPdf) {
        try {
            TccRequest dto = mapper.readValue(dadosJson, TccRequest.class);

            if (dto.getTitulo() == null || dto.getTitulo().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "O título do TCC é obrigatório.", null));
            }
            if (dto.getAlunos() == null || dto.getAlunos().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "O campo 'alunos' é obrigatório.", null));
            }
            if (dto.getCursoId() == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "O ID do curso é obrigatório.", null));
            }

            CursoModel curso = cursoRepository.findById(dto.getCursoId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Curso com ID " + dto.getCursoId() + " não encontrado."));

            TccModel tcc = new TccModel();
            tcc.setTitulo(dto.getTitulo());
            tcc.setAlunos(dto.getAlunos());
            tcc.setOrientadores(dto.getOrientadores());
            tcc.setCurso(curso);
            tcc.setAnoConclusao(dto.getAnoConclusao());
            tcc.setSemestreConclusao(dto.getSemestreConclusao());
            tcc.setLinkExterno(dto.getLinkExterno());
            tcc.setAtivo(dto.getAtivo());

            if (arquivoPdf != null && !arquivoPdf.isEmpty()) {
                String urlPdf = supabaseStorageService.uploadFile(arquivoPdf, "tccs");
                tcc.setArquivoPdf(urlPdf);
            }

            TccModel novoTcc = tccRepository.save(tcc);
            TccResponse response = new TccResponse(novoTcc);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "TCC cadastrado com sucesso.", response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Erro ao cadastrar TCC: " + e.getMessage(), null));
        }
    }

    public ResponseEntity<ApiResponse<List<TccResponse>>> listarTccs() {
        List<TccResponse> tccs = tccRepository.findAll().stream()
                .map(TccResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(true, "Lista de TCCs obtida com sucesso.", tccs));
    }

    public ResponseEntity<ApiResponse<TccResponse>> buscarPorId(Long id) {
        return tccRepository.findById(id)
                .map(tcc -> ResponseEntity.ok(new ApiResponse<>(true, "TCC encontrado.", new TccResponse(tcc))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, "TCC não encontrado.", null)));
    }

    public ResponseEntity<ApiResponse<Void>> excluirTcc(Long id) {
        if (!tccRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, "TCC não encontrado.", null));
        }
        tccRepository.deleteById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "TCC excluído com sucesso.", null));
    }
}