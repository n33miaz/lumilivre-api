package br.com.lumilivre.api.dto.book;

/**
 * Contagem de exemplares de um livro, total e disponíveis, resolvida numa
 * consulta só.
 *
 * <p>Existia como dois métodos separados em {@code BookService}
 * ({@code countAvailableCopies} e {@code countTotalCopies}) que nenhum endpoint
 * chamava — o dado era calculável e nunca chegava ao cliente, e foi por isso que
 * o app mostrou "SEM EXEMPLARES CADASTRADOS" em todo livro para todo leitor.
 * Juntar os dois numa consulta evita que expor a disponibilidade custe duas
 * idas ao banco por ficha.
 */
public record BookCopyCounts(long total, long available) {

    /** Livro sem exemplar nenhum: continua sendo resposta, não ausência de dado. */
    public static final BookCopyCounts NONE = new BookCopyCounts(0, 0);
}
