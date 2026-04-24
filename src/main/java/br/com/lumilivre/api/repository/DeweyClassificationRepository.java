package br.com.lumilivre.api.repository;

import br.com.lumilivre.api.model.DeweyClassification;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.List;

public interface DeweyClassificationRepository extends JpaRepository<DeweyClassification, String> {

    @Cacheable("cdds")
    @Override
    @NonNull
    List<DeweyClassification> findAll();
}
