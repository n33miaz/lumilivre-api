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
        Optional<AppUser> usuarioOpt = appUserRepository.findByEmailOrAluno_Matricula(username, username);

        if (usuarioOpt.isEmpty()) {
            throw new UsernameNotFoundException("UsuÃ¡rio nÃ£o encontrado: " + username);
        }

        return new CustomUserDetails(usuarioOpt.get());
    }
}
