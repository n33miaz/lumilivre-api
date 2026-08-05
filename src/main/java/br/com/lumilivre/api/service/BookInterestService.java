package br.com.lumilivre.api.service;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.dto.book.BookInterestResponse;
import br.com.lumilivre.api.dto.book.BookInterestStateResponse;
import br.com.lumilivre.api.dto.book.BookInterestSummaryResponse;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.mapper.BookMapper;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.BookInterest;
import br.com.lumilivre.api.model.Reader;
import br.com.lumilivre.api.repository.BookInterestRepository;
import br.com.lumilivre.api.repository.BookRepository;
import br.com.lumilivre.api.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;

/**
 * Interesse do leitor por um livro — o "curtir" do app, agora no servidor.
 *
 * <p><b>De onde vem o leitor.</b> Sempre do principal autenticado, nunca do
 * corpo nem da query. Nao existe parametro de matricula em nenhum metodo daqui:
 * o IDOR nao e barrado por validacao, e sim por nao haver o que falsificar. Por
 * isso tambem nao ha {@code @CanAccessReader} — ele compara uma matricula
 * recebida com a do principal, e aqui nao se recebe matricula.
 *
 * <p><b>Sem auditoria.</b> Nem {@code @Auditable} nem {@code @AccessAudited}.
 * O {@code audit_log} guarda quem <i>mudou o acervo</i>, poucas linhas e cada
 * uma relevante isoladamente; marcar interesse nao muda o acervo e, num dia de
 * uso normal, sao centenas de linhas. E o {@code access_log} seria copia:
 * {@code book_interest} ja e um registro de leitor + livro + instante, com a
 * mesma informacao. Registrar a marcacao duas vezes so encheria a trilha.
 *
 * <p>Do lado da remocao, nao guardar rastro tambem e escolha: preferencia
 * retirada por um menor de idade nao deve deixar tumulo.
 */
@Service
@RequiredArgsConstructor
public class BookInterestService {

    private final BookInterestRepository bookInterestRepository;
    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    /**
     * Marcar interesse, idempotente: a segunda chamada devolve o mesmo estado da
     * primeira, com o {@code markedAt} original.
     *
     * <p>Dois nives de protecao contra o duplo clique, de proposito. O
     * {@code findByReader_IdAndBook_Id} resolve o caso normal (o cliente ja tem
     * o coracao preenchido e manda de novo). O {@code catch} da violacao de
     * unicidade resolve a corrida: dois toques quase simultaneos passam os dois
     * pelo SELECT e o segundo INSERT bate no
     * {@code uq_book_interest_reader_book} — sem o catch isso seria um 500 num
     * caminho trivial de app movel, onde duplicar requisicao e rotina.
     */
    @Transactional
    public BookInterestStateResponse marcar(UUID bookId) {
        Reader reader = leitorAutenticado();
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("book.not-found"));

        return bookInterestRepository.findByReader_IdAndBook_Id(reader.getId(), bookId)
                .map(existente -> BookInterestStateResponse.marked(bookId, existente.getCreatedAt()))
                .orElseGet(() -> {
                    try {
                        BookInterest saved = bookInterestRepository.saveAndFlush(BookInterest.builder()
                                .reader(reader)
                                .book(book)
                                .build());
                        return BookInterestStateResponse.marked(bookId, saved.getCreatedAt());
                    } catch (DataIntegrityViolationException concorrencia) {
                        return bookInterestRepository.findByReader_IdAndBook_Id(reader.getId(), bookId)
                                .map(existente -> BookInterestStateResponse.marked(bookId, existente.getCreatedAt()))
                                .orElseThrow(() -> concorrencia);
                    }
                });
    }

    /**
     * Desmarcar, tambem idempotente: desmarcar o que nao estava marcado devolve
     * o mesmo "nao interessado" em vez de 404. O cliente pediu um estado e o
     * estado e esse — 404 aqui obrigaria o app a tratar erro para descrever uma
     * situacao que nao e erro.
     */
    @Transactional
    public BookInterestStateResponse desmarcar(UUID bookId) {
        Reader reader = leitorAutenticado();
        if (!bookRepository.existsById(bookId)) {
            throw ResourceNotFoundException.ofKey("book.not-found");
        }
        bookInterestRepository.deleteByReaderAndBook(reader.getId(), bookId);
        return BookInterestStateResponse.cleared(bookId);
    }

    /** A lista do proprio leitor — o que substitui o {@code SharedPreferences}. */
    @Transactional(readOnly = true)
    public Page<BookInterestResponse> listarDoLeitorAutenticado(Pageable pageable) {
        Reader reader = leitorAutenticado();
        return bookInterestRepository.findMine(reader.getId(), semOrdenacaoDoCliente(pageable))
                .map(interest -> new BookInterestResponse(
                        bookMapper.toCard(interest.getBook()), interest.getCreatedAt()));
    }

    /**
     * O indicador da biblioteca. Agregado: livro, quantos querem, quantos
     * exemplares. Nunca quem.
     *
     * @param unmetOnly restringe aos livros sem nenhum exemplar disponivel — a
     *        pergunta de compra de acervo na sua forma mais direta
     */
    @Transactional(readOnly = true)
    public Page<BookInterestSummaryResponse> resumir(boolean unmetOnly, Pageable pageable) {
        long maxAvailableCopies = unmetOnly ? 0 : Long.MAX_VALUE;
        return bookInterestRepository.summarize(
                BookCopyStatus.AVAILABLE, maxAvailableCopies, semOrdenacaoDoCliente(pageable));
    }

    /**
     * As duas consultas trazem o {@code ORDER BY} escrito no repositorio, e em
     * JPQL o Spring Data <b>anexa</b> o sort do cliente a clausula existente.
     * Deixar passar daria 500 para qualquer campo (viraria
     * {@code ORDER BY ..., alias.campoQualquer}) e, quando o campo existisse,
     * empurraria para tras a ordem que da sentido a pagina. Descartar e mais
     * honesto que fingir uma allowlist: nestas duas rotas nao ha ordem para
     * escolher.
     */
    private Pageable semOrdenacaoDoCliente(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20);
        }
        return pageable.getSort().isUnsorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    /**
     * O leitor do principal corrente.
     *
     * <p>ADMIN e BIBLIOTECARIO nao tem leitor associado, e a barreira de papel
     * ja os impede de chegar aqui; este 400 e a rede para o caso de uma conta de
     * papel READER sem leitor vinculado, que sem ele viraria
     * {@code NullPointerException} e 500.
     */
    private Reader leitorAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            Reader reader = details.getAppUser().getReader();
            if (reader != null) {
                return reader;
            }
        }
        throw BusinessRuleException.ofKey("interest.reader-required");
    }
}
