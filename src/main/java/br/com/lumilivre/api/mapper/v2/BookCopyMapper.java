package br.com.lumilivre.api.mapper.v2;

import java.util.Locale;
import java.util.UUID;

import br.com.lumilivre.api.dto.book.BookCopyRequest;
import br.com.lumilivre.api.dto.book.BookCopyResponse;
import br.com.lumilivre.api.dto.common.LocalizedEnum;
import br.com.lumilivre.api.dto.v1.livro.ExemplarRequest;
import br.com.lumilivre.api.dto.v1.livro.LivroListagemResponse;
import br.com.lumilivre.api.service.EnumLabelResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookCopyMapper {

    private final EnumLabelResolver enumLabels;

    public BookCopyResponse fromV1(LivroListagemResponse v1, UUID bookId, Locale locale) {
        LocalizedEnum status = v1.getStatus() != null
                ? LocalizedEnum.of(v1.getStatus(), enumLabels.resolve(v1.getStatus(), locale))
                : null;
        return BookCopyResponse.builder()
                .copyCode(v1.getTomboExemplar())
                .status(status)
                .bookId(bookId)
                .isbn(v1.getIsbn())
                .title(v1.getNome())
                .author(v1.getAutor())
                .publisher(v1.getEditora())
                .genre(v1.getGenero())
                .deweyCode(v1.getCdd())
                .physicalLocation(v1.getLocalizacao_fisica())
                .build();
    }

    public ExemplarRequest toV1Request(BookCopyRequest req) {
        return ExemplarRequest.builder()
                .tombo(req.getCopyCode())
                .status_livro(req.getStatus())
                .livro_id(req.getBookId())
                .localizacao_fisica(req.getPhysicalLocation())
                .build();
    }
}
