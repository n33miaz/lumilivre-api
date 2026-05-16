package br.com.lumilivre.api.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.dto.v1.livro.ExemplarRequest;
import br.com.lumilivre.api.dto.v1.livro.LivroListagemResponse;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.BookCopy;
import br.com.lumilivre.api.model.Genre;
import br.com.lumilivre.api.repository.BookCopyRepository;
import br.com.lumilivre.api.repository.BookRepository;
import br.com.lumilivre.api.repository.LoanRepository;

@Service
public class BookCopyService {

    private static final Logger log = LoggerFactory.getLogger(BookCopyService.class);

    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    public BookCopyService(BookCopyRepository bookCopyRepository, BookRepository bookRepository,
            LoanRepository loanRepository) {
        this.bookCopyRepository = bookCopyRepository;
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
    }

    public List<BookCopy> buscarTodos() {
        return bookCopyRepository.findAll();
    }

    public List<LivroListagemResponse> buscarExemplaresPorLivroId(UUID bookId) {
        if (bookId == null) {
            throw BusinessRuleException.ofKey("book.copy.book-id.required");
        }
        if (!bookRepository.existsById(bookId)) {
            throw ResourceNotFoundException.ofKey("book.not-found-by-provided-id");
        }

        List<BookCopy> copies = bookCopyRepository.findAllByBookIdWithDetails(bookId);

        return copies.stream()
                .map(this::converterParaDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cadastrar(ExemplarRequest dto) {
        validarDadosExemplar(dto);

        if (bookCopyRepository.existsByCopyCode(dto.getTombo())) {
            throw BusinessRuleException.ofKey("book.copy.code.already-exists");
        }

        Book book = bookRepository.findById(dto.getLivro_id())
                .orElseThrow(() -> ResourceNotFoundException.ofKey("book.not-found-by-provided-id"));

        try {
            BookCopy bookCopy = BookCopy.builder()
                    .copyCode(dto.getTombo())
                    .status(parseStatus(dto.getStatus_livro()))
                    .book(book)
                    .shelfLocation(dto.getLocalizacao_fisica())
                    .build();

            bookCopyRepository.save(bookCopy);
        } catch (Exception e) {
            log.error("Erro ao cadastrar exemplar: {}", e.getMessage(), e);
            throw new RuntimeException("Erro interno ao cadastrar o exemplar: " + e.getMessage());
        }
    }

    @Transactional
    public void atualizar(String copyCode, ExemplarRequest dto) {
        BookCopy bookCopy = bookCopyRepository.findByCopyCode(copyCode)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("book.copy.not-found-by-code", copyCode));

        if (dto.getLivro_id() == null) {
            throw BusinessRuleException.ofKey("book.copy.book-id.required");
        }

        Book newBook = bookRepository.findById(dto.getLivro_id())
                .orElseThrow(() -> ResourceNotFoundException.ofKey("book.not-found-by-provided-id"));

        try {
            bookCopy.setStatus(parseStatus(dto.getStatus_livro()));
            bookCopy.setBook(newBook);
            bookCopy.setShelfLocation(dto.getLocalizacao_fisica());

            bookCopyRepository.save(bookCopy);
        } catch (Exception e) {
            log.error("Erro ao atualizar exemplar {}: {}", copyCode, e.getMessage(), e);
            throw new RuntimeException("Erro interno ao atualizar o exemplar.");
        }
    }

    @Transactional
    public void excluir(String copyCode) {
        BookCopy bookCopy = bookCopyRepository.findByCopyCode(copyCode)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("book.copy.not-found-by-code", copyCode));

        boolean isOnActiveLoan = loanRepository.existsByBookCopy_CopyCodeAndStatusIn(copyCode,
                List.of(LoanStatus.ACTIVE, LoanStatus.OVERDUE));

        if (isOnActiveLoan) {
            throw BusinessRuleException.ofKey("book.copy.cannot-delete-active-loan");
        }

        bookCopyRepository.delete(bookCopy);
    }

    private void validarDadosExemplar(ExemplarRequest dto) {
        if (dto.getLivro_id() == null) {
            throw BusinessRuleException.ofKey("book.copy.book-id.required");
        }
        if (dto.getTombo() == null || dto.getTombo().isBlank()) {
            throw BusinessRuleException.ofKey("book.copy.code.required");
        }
    }

    private BookCopyStatus parseStatus(String status) {
        try {
            return BookCopyStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessRuleException(
                    "Status do exemplar inválido. Valores permitidos: AVAILABLE, BORROWED, UNAVAILABLE, MAINTENANCE.");
        }
    }

    private LivroListagemResponse converterParaDTO(BookCopy copy) {
        Book book = copy.getBook();
        if (book == null) {
            return new LivroListagemResponse(copy.getStatus(), copy.getCopyCode(), "N/A", "N/A",
                    "Livro não associado", "N/A", "N/A", "N/A", copy.getShelfLocation());
        }

        String genres = book.getGenres().stream()
                .map(Genre::getName)
                .collect(Collectors.joining(", "));

        return new LivroListagemResponse(
                copy.getStatus(),
                copy.getCopyCode(),
                book.getIsbn(),
                book.getDeweyClassification() != null ? book.getDeweyClassification().getCode() : "N/A",
                book.getTitle(),
                genres,
                book.getAuthor(),
                book.getPublisher(),
                copy.getShelfLocation());
    }
}
