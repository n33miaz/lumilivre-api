package br.com.lumilivre.api.controller;

import java.util.Locale;
import java.util.UUID;

import br.com.lumilivre.api.dto.user.UserRequest;
import br.com.lumilivre.api.dto.user.UserResponse;
import br.com.lumilivre.api.dto.user.UserSummaryResponse;
import br.com.lumilivre.api.enums.Role;
import br.com.lumilivre.api.mapper.UserMapper;
import br.com.lumilivre.api.service.AppUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AppUserService userService;
    private final UserMapper mapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Page<UserSummaryResponse>> list(
            @RequestParam(required = false) String text,
            Pageable pageable,
            Locale locale) {
        Page<UserSummaryResponse> page = ((text != null && !text.isBlank())
                ? userService.searchUsers(text, pageable)
                : userService.listForAdmin(pageable))
                .map(u -> mapper.toSummary(u, locale));
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(page);
    }

    @GetMapping("/search/advanced")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    public ResponseEntity<Page<UserSummaryResponse>> advancedSearch(
            @RequestParam(required = false) UUID id,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Role role,
            Pageable pageable,
            Locale locale) {
        Page<UserSummaryResponse> page = userService.searchUsersAdvanced(id, email, role, pageable)
                .map(u -> mapper.toSummary(u, locale));
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(page);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest req, Locale locale) {
        UserResponse body = mapper.toResponse(userService.createAdmin(req), locale);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UserRequest req,
            Locale locale) {
        UserResponse body = mapper.toResponse(userService.updateUser(id, req), locale);
        return ResponseEntity.ok()
                .header("Content-Language", locale.toLanguageTag())
                .body(body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
