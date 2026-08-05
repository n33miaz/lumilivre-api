package br.com.lumilivre.api.controller;

import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.lumilivre.api.config.SwaggerTags;
import br.com.lumilivre.api.dto.book.BookInterestResponse;
import br.com.lumilivre.api.dto.book.BookInterestStateResponse;
import br.com.lumilivre.api.dto.book.BookInterestSummaryResponse;
import br.com.lumilivre.api.service.BookInterestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Interesse do leitor por um livro.
 *
 * <p>Fica sob {@code /api/books} porque o recurso é o livro, e num controller
 * próprio porque o {@code BookController} é catálogo e CRUD de acervo — as
 * regras de acesso destas quatro rotas não se parecem com nenhuma das dele: três
 * são exclusivas de LEITOR e uma é exclusiva da equipe.
 *
 * <p><b>Sem parâmetro de leitor em rota nenhuma.</b> O leitor sai do principal
 * autenticado dentro do serviço. É o que fecha o IDOR na origem: não há
 * matrícula, id ou corpo em que trocar o dono do interesse. Consequência
 * deliberada: a ficha do livro é pública desde o T04, mas interesse não é —
 * convidado recebe 401, porque interesse sem dono identificado não é dado.
 *
 * <p><b>Nada de visão nominal.</b> A equipe lê o agregado
 * ({@code interests/summary}); a lista de quem quer o quê não existe como
 * endpoint. Interesse é comportamento de menor de idade e a decisão de compra de
 * acervo se toma com "quantos querem", não com "quem quer".
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Tag(name = SwaggerTags.BOOKS)
public class BookInterestController {

    private final BookInterestService bookInterestService;

    /**
     * Marca interesse. Sempre 200, marcado agora ou já marcado antes.
     *
     * <p>Não é 201/409: interesse é booleano e o cliente pediu um estado, não a
     * criação de um recurso que ele vá endereçar depois. Devolver 409 no segundo
     * toque obrigaria app e web a tratar como erro uma situação em que o estado
     * desejado já vale — e duplo toque em botão de coração é rotina em tela de
     * celular, não exceção.
     */
    @PostMapping("/{id}/interest")
    @Operation(operationId = "interest.toggle")
    @PreAuthorize("hasRole('READER')")
    public ResponseEntity<BookInterestStateResponse> toggle(
            @PathVariable UUID id,
            Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(bookInterestService.marcar(id));
    }

    /**
     * Desmarca interesse. Também idempotente, e com corpo em vez de 204: o
     * cliente lê a mesma forma de resposta do marcar e não precisa de dois
     * caminhos de parse para a mesma tela.
     */
    @DeleteMapping("/{id}/interest")
    @Operation(operationId = "interest.remove")
    @PreAuthorize("hasRole('READER')")
    public ResponseEntity<BookInterestStateResponse> remove(
            @PathVariable UUID id,
            Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(bookInterestService.desmarcar(id));
    }

    @GetMapping("/interests/mine")
    @Operation(operationId = "interest.mine")
    @PreAuthorize("hasRole('READER')")
    public ResponseEntity<Page<BookInterestResponse>> mine(
            @PageableDefault(size = 20) Pageable pageable,
            Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(bookInterestService.listarDoLeitorAutenticado(pageable));
    }

    /**
     * O indicador do painel: livros ordenados por quantos leitores os querem,
     * cruzado com quantos exemplares existem.
     *
     * @param unmetOnly quando verdadeiro, só os livros sem nenhum exemplar
     *        disponível — a fila de compra de acervo
     */
    @GetMapping("/interests/summary")
    @Operation(operationId = "interest.summary")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Page<BookInterestSummaryResponse>> summary(
            @RequestParam(defaultValue = "false") boolean unmetOnly,
            @PageableDefault(size = 20) Pageable pageable,
            Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(bookInterestService.resumir(unmetOnly, pageable));
    }
}
