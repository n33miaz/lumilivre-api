package br.com.lumilivre.api.service;

import br.com.lumilivre.api.data.ApiResponse;
import br.com.lumilivre.api.data.TccRequestDTO;
import br.com.lumilivre.api.data.TccResponseDTO;
import br.com.lumilivre.api.model.TccModel;
import br.com.lumilivre.api.repository.TccRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TccService {

    @Autowired
    private TccRepository tccRepository;

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    private final ObjectMapper mapper = new ObjectMapper();

    public ResponseEntity<?> cadastrarTcc(String dadosJson, MultipartFile arquivoPdf) {
        try {
            TccRequestDTO dto = mapper.readValue(dadosJson, TccRequestDTO.class);

            // Validação básica
            if (dto.getTitulo() == null || dto.getTitulo().isBlank()) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "O título do TCC é obrigatório.", null));
            }
            if (dto.getAlunos() == null || dto.getAlunos().isBlank()) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "O campo 'alunos' é obrigatório.", null));
            }
            if (dto.getCurso() == null || dto.getCurso().isBlank()) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "O curso é obrigatório.", null));
            }

            // Converte DTO para entidade
            TccModel tcc = new TccModel();
            tcc.setTitulo(dto.getTitulo());
            tcc.setAlunos(dto.getAlunos());
            tcc.setOrientadores(dto.getOrientadores());
            tcc.setCurso(dto.getCurso());
            tcc.setAnoConclusao(dto.getAnoConclusao());
            tcc.setSemestreConclusao(dto.getSemestreConclusao());
            tcc.setLinkExterno(dto.getLinkExterno());
            tcc.setAtivo(dto.getAtivo());

            // Upload do arquivo PDF
            if (arquivoPdf != null && !arquivoPdf.isEmpty()) {
                String urlPdf = supabaseStorageService.uploadFile(arquivoPdf, "tccs");
                tcc.setArquivoPdf(urlPdf);
            }

            TccModel novoTcc = tccRepository.save(tcc);
            TccResponseDTO response = toResponseDTO(novoTcc);

            return ResponseEntity.ok(new ApiResponse<>(true, "TCC cadastrado com sucesso.", response));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new ApiResponse<>(false, "Erro ao cadastrar TCC: " + e.getMessage(), null));
        }
    }

    public ResponseEntity<?> listarTccs() {
        List<TccResponseDTO> tccs = tccRepository.findAll().stream().map(this::toResponseDTO).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Lista de TCCs obtida com sucesso.", tccs));
    }

    public ResponseEntity<?> buscarPorId(Long id) {
        Optional<TccModel> tccOpt = tccRepository.findById(id);
        if (tccOpt.isPresent()) {
            return ResponseEntity.ok(new ApiResponse<>(true, "TCC encontrado.", toResponseDTO(tccOpt.get())));
        } else {
            return ResponseEntity.status(404).body(new ApiResponse<>(false, "TCC não encontrado.", null));
        }
    }

    public ResponseEntity<?> excluirTcc(Long id) {
        if (!tccRepository.existsById(id)) {
            return ResponseEntity.status(404).body(new ApiResponse<>(false, "TCC não encontrado.", null));
        }
        tccRepository.deleteById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "TCC excluído com sucesso.", null));
    }

    private TccResponseDTO toResponseDTO(TccModel tcc) {
        return new TccResponseDTO(
                tcc.getId(),
                tcc.getTitulo(),
                tcc.getAlunos(),
                tcc.getOrientadores(),
                tcc.getCurso(),
                tcc.getAnoConclusao(),
                tcc.getSemestreConclusao(),
                tcc.getArquivoPdf(),
                tcc.getLinkExterno(),
                tcc.getAtivo()
        );
    }
}
