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
    public Page<ListaLivroDTO> buscarParaListaAdmin(Pageable pageable) {
        return lr.findLivrosParaListaAdmin(pageable);
    }

    public Iterable<LivroModel> buscarLivrosDisponiveis() {
        return lr.findLivrosDisponiveis();
    }

    public Page<LivroModel> buscarPorTexto(String texto, Pageable pageable) {
        if (texto == null || texto.isBlank()) {
            return lr.findAll(pageable);
        }
        return lr.buscarPorTexto(texto, pageable);
    }

    public Page<LivroModel> buscarAvancado(String nome, String isbn, String autor, String genero, String editora,
            Pageable pageable) {
        return lr.buscarAvancado(nome, isbn, autor, genero, editora, pageable);
    }

    public ResponseEntity<LivroModel> findByIsbn(String isbn) {
        return lr.findByIsbn(isbn)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    public ResponseEntity<List<LivroResponseMobileGeneroDTO>> listarPorGenero(String genero) {
        List<LivroModel> livros = lr.findByGeneroNomeIgnoreCase(genero);

        List<LivroResponseMobileGeneroDTO> resposta = livros.stream()
                .map(l -> new LivroResponseMobileGeneroDTO(
                        l.getImagem(),
                        l.getNome(),
                        l.getAutor()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(resposta);
    }

    public List<LivroModel> buscarTodos() {
        return lr.findAll(); // retorna todos os livros
    }

    public Page<LivroAgrupadoDTO> buscarLivrosAgrupados(Pageable pageable, String texto) {
        return lr.findLivrosAgrupados(pageable, texto); // retorna todos os livros agrupados (agrupa os exemplares)
    }

    public List<GeneroCatalogoDTO> buscarCatalogoParaMobile() {
        List<LivroModel> livrosDisponiveis = lr.findLivrosDisponiveis();

        // Usaremos um Map para agrupar livros por nome de gênero
        Map<String, List<LivroModel>> livrosPorNomeGenero = new HashMap<>();

        // Itera sobre cada livro e seus múltiplos gêneros
        for (LivroModel livro : livrosDisponiveis) {
            for (GeneroModel genero : livro.getGeneros()) {
                // Adiciona o livro à lista correspondente ao nome do gênero
                livrosPorNomeGenero.computeIfAbsent(genero.getNome(), k -> new ArrayList<>()).add(livro);
            }
        }

        List<GeneroCatalogoDTO> catalogo = new ArrayList<>();
        for (Map.Entry<String, List<LivroModel>> entry : livrosPorNomeGenero.entrySet()) {
            String nomeGenero = entry.getKey();
            List<LivroResponseMobileGeneroDTO> livrosDoGenero = entry.getValue().stream()
                    .distinct() // garante que um livro não apareça duas vezes no mesmo gênero
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

        // ordena os carrosséis pelo que tem mais livros
        catalogo.sort((g1, g2) -> Integer.compare(g2.getLivros().size(), g1.getLivros().size()));

        return catalogo;
    }

    // ------------------------ UPLOAD DE CAPA ------------------------
    public ResponseEntity<?> uploadCapa(String isbn, MultipartFile file) {
        Optional<LivroModel> livroOpt = lr.findByIsbn(isbn);
        if (livroOpt.isEmpty()) {
            rm.setMensagem("Livro não encontrado para o ISBN: " + isbn);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(rm);
        }

        LivroModel livro = livroOpt.get();

        try {
            String nomeArquivo = file.getOriginalFilename();
            String url = UrlUtils.gerarUrlValida(BASE_URL_CAPAS, "", nomeArquivo);

            // Upload para o Supabase
            storageService.uploadFile(file);

            // Atualiza apenas a URL da imagem
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
        System.out.println("INICIANDO CADASTRO PARA ISBN: " + dto.getIsbn());
        System.out.println("TÍTULO ENVIADO PELO CLIENTE: " + dto.getNome());

        rm.setMensagem("");

        if (isVazio(dto.getIsbn())) {
            rm.setMensagem("O ISBN é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        if (lr.existsById(dto.getIsbn())) {
            rm.setMensagem("Esse ISBN já está cadastrado.");
            return ResponseEntity.badRequest().body(rm);
        }

        preencherComGoogleBooks(dto);

        ResponseEntity<?> erroValidacao = validarCampos(dto);
        if (erroValidacao != null)
            return erroValidacao;

        LivroModel livro = montarLivro(dto, file);

        lr.save(livro);
        rm.setMensagem("Livro cadastrado com sucesso.");
        System.out.println("✅ LIVRO CADASTRADO COM SUCESSO: " + dto.getNome());
        return ResponseEntity.status(HttpStatus.CREATED).body(rm);
    }

    // ------------------------ ATUALIZAÇÃO ------------------------
    public ResponseEntity<?> atualizar(LivroDTO dto, MultipartFile file) {
        rm.setMensagem("");

        if (isVazio(dto.getIsbn())) {
            rm.setMensagem("O ISBN é obrigatório.");
            return ResponseEntity.badRequest().body(rm);
        }

        Optional<LivroModel> livroExistente = lr.findByIsbn(dto.getIsbn());
        if (livroExistente.isEmpty()) {
            rm.setMensagem("Livro não encontrado.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(rm);
        }

        preencherComGoogleBooks(dto);

        ResponseEntity<?> erroValidacao = validarCampos(dto);
        if (erroValidacao != null)
            return erroValidacao;

        LivroModel livro = montarLivro(dto, file);
        livro.setIsbn(dto.getIsbn()); // garante que o ISBN não muda

        lr.save(livro);
        rm.setMensagem("Livro atualizado com sucesso.");
        return ResponseEntity.ok(rm);
    }

    // ------------------------ MÉTODOS AUXILIARES ------------------------
    private void preencherComGoogleBooks(LivroDTO dto) {
        System.out.println("BUSCANDO ISBN NO GOOGLE BOOKS: " + dto.getIsbn());

        LivroModel livroGoogle = googleBooksService.buscarLivroPorIsbn(dto.getIsbn());

        if (livroGoogle == null) {
            System.out.println("LIVRO NÃO ENCONTRADO NO GOOGLE BOOKS OU ERRO NA CONEXÃO");
            return;
        }

        System.out.println("LIVRO ENCONTRADO: " + livroGoogle.getNome());
        System.out.println("DADOS DA API GOOGLE BOOKS:");
        System.out.println("   Título: " + livroGoogle.getNome());
        System.out.println("   Autor: " + livroGoogle.getAutor());
        System.out.println("   Editora: " + livroGoogle.getEditora());
        System.out.println("   Páginas: " + livroGoogle.getNumero_paginas());
        System.out.println("   Data: " + livroGoogle.getData_lancamento());

        // Preenche apenas campos que estão vazios/no DTO
        if (isVazio(dto.getNome())) {
            System.out.println("Preenchendo título: " + livroGoogle.getNome());
            dto.setNome(livroGoogle.getNome());
        }
        if (isVazio(dto.getEditora())) {
            System.out.println("Preenchendo editora: " + livroGoogle.getEditora());
            dto.setEditora(livroGoogle.getEditora());
        }
        if (dto.getNumero_paginas() == null) {
            System.out.println("Preenchendo páginas: " + livroGoogle.getNumero_paginas());
            dto.setNumero_paginas(livroGoogle.getNumero_paginas());
        }
        if (dto.getData_lancamento() == null) {
            System.out.println("Preenchendo data: " + livroGoogle.getData_lancamento());
            dto.setData_lancamento(livroGoogle.getData_lancamento());
        }
        if (isVazio(dto.getSinopse())) {
            System.out.println("Preenchendo sinopse");
            dto.setSinopse(livroGoogle.getSinopse());
        }
        if (isVazio(dto.getImagem())) {
            System.out.println("Preenchendo imagem");
            dto.setImagem(livroGoogle.getImagem());
        }
        if (isVazio(dto.getAutor()) && !isVazio(livroGoogle.getAutor())) {
            System.out.println("👥 Preenchendo autor: " + livroGoogle.getAutor());
            dto.setAutor(livroGoogle.getAutor());
        }

        if (dto.getGeneros() == null || dto.getGeneros().isEmpty()) {
            List<String> categoriasDoGoogle = googleBooksService.getCategories();
            if (categoriasDoGoogle != null && !categoriasDoGoogle.isEmpty()) {
                System.out.println("Preenchendo gêneros: " + categoriasDoGoogle);
                dto.setGeneros(new HashSet<>(categoriasDoGoogle));
            }
        }

        System.out.println("DADOS APÓS PREENCHIMENTO:");
        System.out.println("   Título: " + dto.getNome());
        System.out.println("   Autor: " + dto.getAutor());
        System.out.println("   Editora: " + dto.getEditora());
    }

    private ResponseEntity<?> validarCampos(LivroDTO dto) {
        System.out.println("VALIDANDO CAMPOS DO DTO:");
        System.out.println("   Título: " + dto.getNome());
        System.out.println("   Autor: " + dto.getAutor());
        System.out.println("   Editora: " + dto.getEditora());
        System.out.println("   Data: " + dto.getData_lancamento());
        System.out.println("   Páginas: " + dto.getNumero_paginas());
        System.out.println("   CDD: " + dto.getCdd());
        System.out.println("   Gênero: " + dto.getGeneros());

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
        if (dto.getGeneros() == null || dto.getGeneros().isEmpty())
            return erro("O gênero é obrigatório.");

        System.out.println("VALIDAÇÃO OK");
        return null;
    }

    private LivroModel montarLivro(LivroDTO dto, MultipartFile file) {
        LivroModel livro = new LivroModel();
        livro.setIsbn(dto.getIsbn());
        livro.setNome(dto.getNome());
        livro.setData_lancamento(dto.getData_lancamento());
        livro.setNumero_paginas(dto.getNumero_paginas());

        CddModel cdd = cddRepository.findById(dto.getCdd())
                .orElseThrow(() -> new IllegalArgumentException("Código CDD inválido: " + dto.getCdd()));
        livro.setCdd(cdd);

        try {
            livro.setClassificacao_etaria(ClassificacaoEtaria.valueOf(dto.getClassificacao_etaria().toUpperCase()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Classificação etária inválida: " + dto.getClassificacao_etaria());
        }
        try {
            livro.setClassificacao_etaria(ClassificacaoEtaria.valueOf(dto.getClassificacao_etaria().toUpperCase()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Classificação etária inválida: " + dto.getClassificacao_etaria());
        }
        try {
            livro.setTipo_capa(TipoCapa.valueOf(dto.getTipo_capa().toUpperCase()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Tipo de capa inválido: " + dto.getTipo_capa());
        }

        livro.setEditora(dto.getEditora());
        livro.setEdicao(dto.getEdicao());
        livro.setVolume(dto.getVolume());
        livro.setQuantidade(dto.getQuantidade());
        livro.setSinopse(dto.getSinopse());
        livro.setAutor(dto.getAutor());

        Set<GeneroModel> generos = processarGeneros(dto.getCdd());
        livro.setGeneros(generos);

        // Imagem
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
        System.out.println("ERRO DE VALIDAÇÃO: " + mensagem);
        rm.setMensagem(mensagem);
        return ResponseEntity.badRequest().body(rm);
    }

    private Set<GeneroModel> processarGeneros(String cddCodigo) {
        if (cddCodigo == null || cddCodigo.isBlank()) {
            return new HashSet<>();
        }
        // Usa o novo método do repositório para encontrar todos os gêneros associados
        // ao CDD
        return gr.findAllByCddCodigo(cddCodigo);
    }

    // ------------------------ EXCLUSÃO ------------------------
    public ResponseEntity<ResponseModel> excluir(String isbn) {
        if (!lr.existsById(isbn)) {
            rm.setMensagem("Livro não encontrado.");
            return ResponseEntity.badRequest().body(rm);
        }
        lr.deleteById(isbn);
        rm.setMensagem("Livro removido com sucesso.");
        return ResponseEntity.ok(rm);
    }

    @Transactional
    public ResponseEntity<?> excluirLivroComExemplares(String isbn) {
        Optional<LivroModel> livroOpt = lr.findByIsbn(isbn);
        if (livroOpt.isEmpty())
            return erro("Livro não encontrado.");

        er.deleteAllByLivroIsbn(isbn);
        lr.delete(livroOpt.get());

        rm.setMensagem("Livro e todos os exemplares foram removidos com sucesso.");
        return ResponseEntity.ok(rm);
    }

    @Transactional
    public void atualizarQuantidadeExemplaresDoLivro(String isbn) {
        Long quantidade = er.contarExemplaresPorLivro(isbn);
        lr.findById(isbn).ifPresent(l -> {
            l.setQuantidade(quantidade.intValue());
            lr.save(l);
        });
    }
}