package br.com.lumilivre.api.security;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import br.com.lumilivre.api.model.AppUser;
import br.com.lumilivre.api.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<AppUser> appUser = appUserRepository.findByEmailOrRegistrationNumber(username, username);

        if (appUser.isEmpty()) {
            throw new UsernameNotFoundException("Usuário não encontrado: " + username);
        }

        return new CustomUserDetails(appUser.get());
    }
}
