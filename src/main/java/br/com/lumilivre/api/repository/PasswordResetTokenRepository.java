package br.com.lumilivre.api.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.lumilivre.api.model.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Apaga os tokens anteriores do usuário. Além de invalidar o link antigo
     * (SEC-23), é obrigatório para o insert seguinte: a tabela tem UNIQUE em
     * app_user_id, então pedir um segundo reset sem limpar estourava constraint.
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.appUser.id = :appUserId")
    int deleteByAppUserId(@Param("appUserId") UUID appUserId);
}
