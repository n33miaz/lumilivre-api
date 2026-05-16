package br.com.lumilivre.api.controller.v2;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.com.lumilivre.api.dto.common.AddressLookupResponse;
import br.com.lumilivre.api.dto.common.LocalizedEnum;
import br.com.lumilivre.api.dto.metadata.AuthorSummaryResponse;
import br.com.lumilivre.api.dto.metadata.PostalCodeResponse;
import br.com.lumilivre.api.enums.AgeRating;
import br.com.lumilivre.api.enums.BookCopyStatus;
import br.com.lumilivre.api.enums.CoverType;
import br.com.lumilivre.api.enums.LoanRequestStatus;
import br.com.lumilivre.api.enums.LoanStatus;
import br.com.lumilivre.api.enums.PenaltyCode;
import br.com.lumilivre.api.enums.ReservationStatus;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.exception.custom.BusinessRuleException;
import br.com.lumilivre.api.exception.custom.ResourceNotFoundException;
import br.com.lumilivre.api.repository.BookRepository;
import br.com.lumilivre.api.service.EnumLabelResolver;
import br.com.lumilivre.api.service.infra.CepService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/metadata")
@RequiredArgsConstructor
public class MetadataController {

    private final EnumLabelResolver enumLabelResolver;
    private final BookRepository bookRepository;
    private final CepService cepService;

    @GetMapping("/enums/{type}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    public ResponseEntity<List<LocalizedEnum>> enums(@PathVariable String type, Locale locale) {
        List<LocalizedEnum> body = switch (normalizeType(type)) {
            case "STATUS_LIVRO", "BOOK_COPY_STATUS" -> localizedValues(BookCopyStatus.class, locale);
            case "STATUS_EMPRESTIMO", "LOAN_STATUS" -> localizedValues(LoanStatus.class, locale);
            case "PENALIDADE", "PENALTY_CODE" -> localizedValues(PenaltyCode.class, locale);
            case "TIPO_CAPA", "COVER_TYPE" -> localizedValues(CoverType.class, locale);
            case "CLASSIFICACAO_ETARIA", "AGE_RATING" -> localizedValues(AgeRating.class, locale);
            case "RESERVATION_STATUS" -> localizedValues(ReservationStatus.class, locale);
            case "LOAN_REQUEST_STATUS" -> localizedValues(LoanRequestStatus.class, locale);
            case "ROLE" -> localizedValues(Role.class, locale);
            default -> throw BusinessRuleException.ofKey("metadata.enum-type.unsupported", type);
        };
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @GetMapping("/authors")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    public ResponseEntity<Page<AuthorSummaryResponse>> authors(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable,
            Locale locale) {
        List<AuthorSummaryResponse> authors = bookRepository.countByAutor()
                .stream()
                .map(this::toAuthorSummary)
                .filter(author -> matchesQuery(author.name(), q))
                .toList();
        int start = Math.min((int) pageable.getOffset(), authors.size());
        int end = Math.min(start + pageable.getPageSize(), authors.size());
        Page<AuthorSummaryResponse> page = new PageImpl<>(
                authors.subList(start, end),
                pageable,
                authors.size());
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(page);
    }

    @GetMapping("/postal-codes/{postalCode}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','STUDENT')")
    public ResponseEntity<PostalCodeResponse> postalCode(
            @PathVariable String postalCode,
            Locale locale) {
        String normalized = postalCode.replaceAll("\\D", "");
        if (normalized.length() != 8) {
            throw BusinessRuleException.ofKey("metadata.postal-code.invalid");
        }
        AddressLookupResponse address = cepService.buscarEnderecoPorCep(normalized);
        if (address == null || isBlank(address.getLogradouro()) || isBlank(address.getLocalidade())) {
            throw ResourceNotFoundException.ofKey("metadata.postal-code.not-found", normalized);
        }
        PostalCodeResponse body = new PostalCodeResponse(
                normalized,
                address.getLogradouro(),
                address.getComplemento(),
                address.getBairro(),
                address.getLocalidade(),
                address.getUf());
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    private <E extends Enum<E>> List<LocalizedEnum> localizedValues(Class<E> enumType, Locale locale) {
        return Arrays.stream(enumType.getEnumConstants())
                .map(value -> LocalizedEnum.of(value, enumLabelResolver.resolve(value, locale)))
                .toList();
    }

    private String normalizeType(String type) {
        return type == null
                ? ""
                : type.trim()
                        .replace('-', '_')
                        .replace(' ', '_')
                        .toUpperCase(Locale.ROOT);
    }

    private AuthorSummaryResponse toAuthorSummary(Map<String, Object> row) {
        Object total = row.get("total");
        return new AuthorSummaryResponse(
                String.valueOf(row.get("autor")),
                total instanceof Number number ? number.longValue() : 0L);
    }

    private boolean matchesQuery(String value, String query) {
        if (isBlank(query)) {
            return true;
        }
        return value != null && value.toLowerCase(Locale.ROOT)
                .contains(query.trim().toLowerCase(Locale.ROOT));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
