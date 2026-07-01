package br.com.lumilivre.api.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import br.com.lumilivre.api.config.SwaggerTags;
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
import br.com.lumilivre.api.service.EnumLabelResolver;
import br.com.lumilivre.api.service.MetadataService;
import br.com.lumilivre.api.service.infra.postalcode.PostalAddress;
import br.com.lumilivre.api.service.infra.postalcode.PostalCodeRouter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
@RequestMapping("/api/metadata")
@RequiredArgsConstructor
@Tag(name = SwaggerTags.METADATA)
public class MetadataController {

    private final EnumLabelResolver enumLabelResolver;
    private final MetadataService metadataService;
    private final PostalCodeRouter postalCodeRouter;

    @GetMapping("/enums/{type}")
    @Operation(operationId = "metadata.enums")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','READER')")
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
    @Operation(operationId = "metadata.authors")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','READER')")
    public ResponseEntity<Page<AuthorSummaryResponse>> authors(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable,
            Locale locale) {
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(metadataService.authors(q, pageable));
    }

    @GetMapping("/postal-codes/{postalCode}")
    @Operation(operationId = "metadata.postalCode")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN','READER')")
    public ResponseEntity<PostalCodeResponse> postalCode(
            @PathVariable String postalCode,
            @RequestParam(value = "country", required = false, defaultValue = "BR") String countryCode,
            Locale locale) {
        String iso = countryCode == null ? "BR" : countryCode.trim().toUpperCase(Locale.ROOT);
        String normalized = "BR".equals(iso) ? postalCode.replaceAll("\\D", "") : postalCode.trim();
        if ("BR".equals(iso) && normalized.length() != 8) {
            throw BusinessRuleException.ofKey("metadata.postal-code.invalid");
        }
        if (normalized.isBlank()) {
            throw BusinessRuleException.ofKey("metadata.postal-code.invalid");
        }
        PostalAddress address = postalCodeRouter.lookup(normalized, iso)
                .orElseThrow(() -> ResourceNotFoundException.ofKey("metadata.postal-code.not-found", normalized));
        PostalCodeResponse body = new PostalCodeResponse(
                address.postalCode(),
                address.street(),
                address.addressComplement(),
                address.district(),
                address.city(),
                address.regionCode());
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

}
