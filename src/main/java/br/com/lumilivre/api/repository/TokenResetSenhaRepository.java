package br.com.lumilivre.api.repository;

import br.com.lumilivre.api.model.TokenResetSenhaModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TokenResetSenhaRepository extends JpaRepository<TokenResetSenhaModel, Long> {
    Optional<TokenResetSenhaModel> findByToken(String token);
}