package br.com.lumilivre.api.dto.book;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma linha do indicador de interesse da biblioteca: um livro, quantas pessoas
 * o querem e quantos exemplares existem para atender.
 *
 * <p><b>Agregado por decisão, não por conveniência.</b> Interesse é dado de
 * comportamento de menor de idade e este recorte é o que a bibliotecária vê.
 * A decisão de compra de acervo se responde com "quantos querem" e "quantos
 * temos"; ela não fica melhor sabendo <i>quem</i> quer. Por isso não existe
 * campo de leitor aqui, nem endpoint que devolva a lista nominal — nem para
 * BIBLIOTECARIO, nem para ADMIN. O único ponto do sistema onde interesse
 * aparece ligado a uma pessoa é a lista do próprio leitor
 * ({@link BookInterestResponse}), atrás do seu próprio token.
 *
 * <p>{@code interestCount} conta leitores distintos por construção: a V8 tem
 * {@code UNIQUE (reader_id, book_id)}, então uma linha por leitor é o máximo.
 *
 * <p>{@code availableCopies} é o cruzamento que dá utilidade ao número: "18
 * alunos querem, temos 1 exemplar" é a frase que decide a compra. Zero
 * disponível com interesse alto é o caso que o painel precisa destacar.
 */
public record BookInterestSummaryResponse(
        UUID bookId,
        String title,
        String author,
        String coverUrl,
        OffsetDateTime updatedAt,
        long interestCount,
        long totalCopies,
        long availableCopies) {
}
