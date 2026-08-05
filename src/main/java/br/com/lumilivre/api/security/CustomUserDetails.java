package br.com.lumilivre.api.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import br.com.lumilivre.api.model.AppUser;

public class CustomUserDetails implements UserDetails {

    private final AppUser appUser;

    public CustomUserDetails(AppUser appUser) {
        this.appUser = appUser;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name()));
    }

    @Override
    public String getPassword() {
        return appUser.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return appUser.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        // Conta não expira por tempo; o que expira é o token (jwt.expiration).
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !Boolean.TRUE.equals(appUser.getLocked());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // A troca obrigatória de senha é tratada pelo MustChangePasswordFilter,
        // que só barra parte das rotas — aqui devolver false derrubaria o login
        // inteiro e o usuário não teria como trocar a senha.
        return true;
    }

    @Override
    public boolean isEnabled() {
        // Conta "excluída" (soft delete) é conta desativada: antes o login
        // ignorava deleted_at e um desligado continuava entrando.
        return Boolean.TRUE.equals(appUser.getActive()) && appUser.getDeletedAt() == null;
    }

    public AppUser getAppUser() {
        return this.appUser;
    }
}
