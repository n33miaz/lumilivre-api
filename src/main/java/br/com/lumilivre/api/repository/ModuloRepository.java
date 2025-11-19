package br.com.lumilivre.api.repository;

import br.com.lumilivre.api.model.ModuloModel;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.List;

public interface ModuloRepository extends JpaRepository<ModuloModel, Integer> {

    @Cacheable("modulos")
    @Override
    @NonNull
    List<ModuloModel> findAll();
}