package br.com.lumilivre.api.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.com.lumilivre.api.data.ListaLivroDTO;
import br.com.lumilivre.api.data.LivroAgrupadoDTO;
import br.com.lumilivre.api.data.LivroDTO;
import br.com.lumilivre.api.data.LivroResponseMobileGeneroDTO;
import br.com.lumilivre.api.data.GeneroCatalogoDTO;
import br.com.lumilivre.api.model.CddModel;
import br.com.lumilivre.api.repository.CddRepository;
import br.com.lumilivre.api.enums.ClassificacaoEtaria;
import br.com.lumilivre.api.enums.TipoCapa;
import br.com.lumilivre.api.model.GeneroModel;
import br.com.lumilivre.api.model.LivroModel;
import br.com.lumilivre.api.model.ResponseModel;
import br.com.lumilivre.api.repository.ExemplarRepository;
import br.com.lumilivre.api.repository.GeneroRepository;
import br.com.lumilivre.api.repository.LivroRepository;
import br.com.lumilivre.api.utils.UrlUtils;

@Service
public class LivroService {

    @Autowired
    private ExemplarRepository er;
    @Autowired
    private LivroRepository lr;
    @Autowired
    private ResponseModel rm;
    @Autowired
    private SupabaseStorageService storageService;
    @Autowired
    private GoogleBooksService googleBooksService;
    @Autowired
    private GeneroRepository gr;
    @Autowired
    private CddRepository cddRepository;

    private final String BASE_URL_CAPAS = "https://ylwmaozotaddmyhosiqc.supabase.co/storage/v1/object/capas/livros";

    // ------------------------ BUSCAS ------------------------
    public List<LivroModel> buscarTodos() {
        return lr.findAll();
    }

    public Page<ListaLivroDTO> buscarParaListaAdmin(Pageable pageable) {
        return lr.findLivrosParaListaAdmin(pageable);
    }

    public Page<LivroAgrupadoDTO> buscarLivrosAgrupados(Pageable pageable, String texto) {
        return lr.findLivrosAgrupados(pageable, texto);
    }

    public ResponseEntity<LivroModel> findById(Long id) {
        return lr.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    public List<GeneroCatalogoDTO> buscarCatalogoParaMobile() {
        List<LivroModel> livrosDisponiveis = lr.findLivrosDisponiveis();
        Map<String, List<LivroModel>> livrosPorNomeGenero = new HashMap<>();

        for (LivroModel livro : livrosDisponiveis) {
            for (GeneroModel genero : livro.getGeneros()) {
                livrosPorNomeGenero.computeIfAbsent(genero.getNome(), k -> new ArrayList<>()).add(livro);
            }
        }

        List<GeneroCatalogoDTO> catalogo = new ArrayList<>();
        for (Map.Entry<String, List<LivroModel>> entry : livrosPorNomeGenero.entrySet()) {
            String nomeGenero = entry.getKey();
            List<LivroResponseMobileGeneroDTO> livrosDoGenero = entry.getValue().stream()
                    .distinct()
                    .limit(10)
                    .map(livro -> new LivroResponseMobileGeneroDTO(
                            livro.getImagem(),
                            livro.getNome(),
                            livro.getAutor()))
                    .collect(Collectors.toList());

            if (!livrosDoGenero.isEmpty()) {
                catalogo.add(new GeneroCatalogoDTO(nomeGenero, livrosDoGenero));
            }
        }
        catalogo.sort((g1, g2) -> Integer.compare(g2.getLivros().size(), g1.getLivros().size()));
        return catalogo;
    }

    // ------------------------ UPLOAD DE CAPA ------------------------
    public ResponseEntity<?> uploadCapa(Long id, MultipartFile file) {
        Optional<LivroModel> livroOpt = lr.findById(id);
        if (livroOpt.isEmpty()) {
            rm.setMensagem("Livro não encontrado para o ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(rm);
        }

        LivroModel livro = livroOpt.get();
        try {
            String nomeArquivo = file.getOriginalFilename();
            String url = UrlUtils.gerarUrlValida(BASE_URL_CAPAS, "", nomeArquivo);
            storageService.uploadFile(file);
            livro.setImagem(url);
            lr.save(livro);
            rm.setMensagem("Capa atualizada com sucesso.");
            return ResponseEntity.ok(rm);
        } catch (Exception e) {
            rm.setMensagem("Erro ao fazer upload da capa: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(rm);
        }
    }

    // ------------------------ CADASTRO ------------------------
    public ResponseEntity<?> cadastrar(LivroDTO dto, MultipartFile file) {
        rm.setMensagem("");

        if (!isVazio(dto.getIsbn()) && lr.findByIsbn(dto.getIsbn()).isPresent()) {
            rm.setMensagem("Esse ISBN já está cadastrado em outro livro.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (!isVazio(dto.getIsbn())) {
            preencherComGoogleBooks(dto);
        }

        ResponseEntity<?> erroValidacao = validarCampos(dto);
        if (erroValidacao != null)
            return erroValidacao;

        LivroModel livro = montarLivro(dto, file, null);

        lr.save(livro);
        rm.setMensagem("Livro cadastrado com sucesso.");
        return ResponseEntity.status(HttpStatus.CREATED).body(rm);
    }

    // ------------------------ ATUALIZAÇÃO ------------------------
    public ResponseEntity<?> atualizar(Long id, LivroDTO dto, MultipartFile file) {
        rm.setMensagem("");

        Optional<LivroModel> livroExistenteOpt = lr.findById(id);
        if (livroExistenteOpt.isEmpty()) {
            rm.setMensagem("Livro não encontrado para o ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(rm);
        }

        if (!isVazio(dto.getIsbn())) {
            Optional<LivroModel> livroComMesmoIsbn = lr.findByIsbn(dto.getIsbn());
            if (livroComMesmoIsbn.isPresent() && !livroComMesmoIsbn.get().getId().equals(id)) {
                rm.setMensagem("O ISBN informado já pertence a outro livro.");
                return ResponseEntity.badRequest().body(rm);
            }
        }

        ResponseEntity<?> erroValidacao = validarCampos(dto);
        if (erroValidacao != null)
            return erroValidacao;

        LivroModel livro = montarLivro(dto, file, id);

        lr.save(livro);
        rm.setMensagem("Livro atualizado com sucesso.");
        return ResponseEntity.ok(rm);
    }

    // ------------------------ MÉTODOS AUXILIARES ------------------------
    private void preencherComGoogleBooks(LivroDTO dto) {
        // Esta lógica continua a mesma, mas agora é chamada condicionalmente.
        LivroModel livroGoogle = googleBooksService.buscarLivroPorIsbn(dto.getIsbn());
        if (livroGoogle == null)
            return;

        if (isVazio(dto.getNome()))
            dto.setNome(livroGoogle.getNome());
        if (isVazio(dto.getEditora()))
            dto.setEditora(livroGoogle.getEditora());
        if (dto.getNumero_paginas() == null)
            dto.setNumero_paginas(livroGoogle.getNumero_paginas());
        if (dto.getData_lancamento() == null)
            dto.setData_lancamento(livroGoogle.getData_lancamento());
        if (isVazio(dto.getSinopse()))
            dto.setSinopse(livroGoogle.getSinopse());
        if (isVazio(dto.getImagem()))
            dto.setImagem(livroGoogle.getImagem());
        if (isVazio(dto.getAutor()) && !isVazio(livroGoogle.getAutor()))
            dto.setAutor(livroGoogle.getAutor());
    }

    private ResponseEntity<?> validarCampos(LivroDTO dto) {
        if (isVazio(dto.getNome()))
            return erro("O título é obrigatório.");
        if (dto.getData_lancamento() == null)
            return erro("A data é obrigatória.");
        if (dto.getData_lancamento().isAfter(LocalDate.now()))
            return erro("A data de lançamento não pode ser no futuro.");
        if (dto.getNumero_paginas() == null || dto.getNumero_paginas() <= 0)
            return erro("O número de páginas é obrigatório.");
        if (isVazio(dto.getEditora()))
            return erro("A editora é obrigatória.");
        if (isVazio(dto.getCdd()))
            return erro("O CDD é obrigatório.");
        if (isVazio(dto.getAutor()))
            return erro("O autor é obrigatório.");

        return null;
    }

    private LivroModel montarLivro(LivroDTO dto, MultipartFile file, Long id) {
        LivroModel livro = new LivroModel();
        if (id != null) {
            livro.setId(id);
        }

        livro.setIsbn(dto.getIsbn());
        livro.setNome(dto.getNome());
        livro.setData_lancamento(dto.getData_lancamento());
        livro.setNumero_paginas(dto.getNumero_paginas());

        CddModel cdd = cddRepository.findById(dto.getCdd())
                .orElseThrow(() -> new IllegalArgumentException("Código CDD inválido: " + dto.getCdd()));
        livro.setCdd(cdd);

        try {
            livro.setClassificacao_etaria(ClassificacaoEtaria.valueOf(dto.getClassificacao_etaria().toUpperCase()));
            livro.setTipo_capa(TipoCapa.valueOf(dto.getTipo_capa().toUpperCase()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Classificação etária ou Tipo de capa inválido.");
        }

        livro.setEditora(dto.getEditora());
        livro.setEdicao(dto.getEdicao());
        livro.setVolume(dto.getVolume());
        livro.setQuantidade(dto.getQuantidade());
        livro.setSinopse(dto.getSinopse());
        livro.setAutor(dto.getAutor());

        Set<GeneroModel> generos = processarGeneros(dto.getCdd());
        livro.setGeneros(generos);

        if (file != null && !file.isEmpty()) {
            try {
                String nomeArquivo = file.getOriginalFilename();
                String url = UrlUtils.gerarUrlValida(BASE_URL_CAPAS, "", nomeArquivo);
                livro.setImagem(url);
                storageService.uploadFile(file);
            } catch (Exception e) {
                throw new RuntimeException("Erro ao enviar a capa: " + e.getMessage(), e);
            }
        } else if (!isVazio(dto.getImagem())) {
            livro.setImagem(dto.getImagem());
        }

        return livro;
    }

    private boolean isVazio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private ResponseEntity<?> erro(String mensagem) {
        rm.setMensagem(mensagem);
        return ResponseEntity.badRequest().body(rm);
    }

    private Set<GeneroModel> processarGeneros(String cddCodigo) {
        if (cddCodigo == null || cddCodigo.isBlank()) {
            return new HashSet<>();
        }
        return gr.findAllByCddCodigo(cddCodigo);
    }

    // ------------------------ EXCLUSÃO ------------------------
    @Transactional
    public ResponseEntity<?> excluirLivroComExemplares(Long id) {
        Optional<LivroModel> livroOpt = lr.findById(id);
        if (livroOpt.isEmpty()) {
            return erro("Livro não encontrado.");
        }
        er.deleteAllByLivroId(id);
        lr.delete(livroOpt.get());
        rm.setMensagem("Livro e todos os exemplares foram removidos com sucesso.");
        return ResponseEntity.ok(rm);
    }

    @Transactional
    public void atualizarQuantidadeExemplaresDoLivro(Long livroId) {
        if (livroId == null)
            return;
        Long quantidade = er.countByLivroId(livroId);
        lr.findById(livroId).ifPresent(livro -> {
            livro.setQuantidade(quantidade.intValue());
            lr.save(livro);
        });
    }
}