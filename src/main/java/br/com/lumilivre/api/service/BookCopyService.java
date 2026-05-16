package br.com.lumilivre.api.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.lumilivre.api.dto.book.BookCopyRequest;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.model.Book;
import br.com.lumilivre.api.model.BookCopy;
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

    public List<BookCopy> buscarExemplaresPorLivroId(UUID bookId) {
        if (bookId == null) {
            throw BusinessRuleException.ofKey("book.copy.book-id.required");
        }
        if (!bookRepository.existsById(bookId)) {
            throw ResourceNotFoundException.ofKey("book.not-found-by-provided-id");
        }

        return bookCopyRepository.findAllByBookIdWithDetails(bookId);
    }

    @Transactional
    public void cadastrar(BookCopyRequest request) {
        validarDadosExemplar(request);

        if (bookCopyRepository.existsByCopyCode(request.getCopyCode())) {
            throw BusinessRuleException.ofKey("book.copy.code.already-exists");
        }

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> ResourceNotFoundException.ofKey("book.not-found-by-provided-id"));

        try {
            BookCopy bookCopy = BookCopy.builder()
                    .copyCode(request.getCopyCode())
                    .status(parseStatus(request.getStatus()))
                    .book(book)
                    .shelfLocation(request.getPhysicalLocation())
                    .build();

            bookCopyRepository.save(bookCopy);
        } catch (Exception e) {
            log.error("Erro ao cadastrar exemplar: {}", e.getMessage(), e);
            throw new RuntimeException("Erro interno ao cadastrar o exemplar: " + e.getMessage());
        }
    }

    @Transactional
    public void atualizar(String copyCode, BookCopyRequest request) {
        BookCopy bookCopy = bookCopyRepository.findByCopyCode(copyCode)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("book.copy.not-found-by-code", copyCode));

        if (request.getBookId() == null) {
            throw BusinessRuleException.ofKey("book.copy.book-id.required");
        }

        Book newBook = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> ResourceNotFoundException.ofKey("book.not-found-by-provided-id"));

        try {
            bookCopy.setStatus(parseStatus(request.getStatus()));
            bookCopy.setBook(newBook);
            bookCopy.setShelfLocation(request.getPhysicalLocation());

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

    private void validarDadosExemplar(BookCopyRequest request) {
        if (request.getBookId() == null) {
            throw BusinessRuleException.ofKey("book.copy.book-id.required");
        }
        if (request.getCopyCode() == null || request.getCopyCode().isBlank()) {
            throw BusinessRuleException.ofKey("book.copy.code.required");
        }
    }

    private BookCopyStatus parseStatus(String status) {
        try {
            return BookCopyStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw BusinessRuleException.ofKey("book.copy.status.invalid");
        }
    }
}
