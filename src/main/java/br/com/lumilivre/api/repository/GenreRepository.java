package br.com.lumilivre.api.repository;

import br.com.lumilivre.api.model.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface GenreRepository extends JpaRepository<Genre, Integer> {

    @Override
    @NonNull
    List<Genre> findAll();

    Optional<Genre> findByNameIgnoreCase(String name);

    Set<Genre> findByNameIn(Set<String> names);
}
