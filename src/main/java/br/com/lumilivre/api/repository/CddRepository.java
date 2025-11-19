package br.com.lumilivre.api.repository;

import br.com.lumilivre.api.model.CddModel;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.List;

public interface CddRepository extends JpaRepository<CddModel, String> {

    @Cacheable("cdds")
    @Override
    @NonNull
    List<CddModel> findAll();
}