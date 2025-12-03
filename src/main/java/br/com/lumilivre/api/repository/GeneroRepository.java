package br.com.lumilivre.api.repository;

import br.com.lumilivre.api.model.GeneroModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface GeneroRepository extends JpaRepository<GeneroModel, Integer> {

    @Override
    @NonNull
    List<GeneroModel> findAll();

    Optional<GeneroModel> findByNomeIgnoreCase(String nome);

    Set<GeneroModel> findByNomeIn(Set<String> nomes);
}