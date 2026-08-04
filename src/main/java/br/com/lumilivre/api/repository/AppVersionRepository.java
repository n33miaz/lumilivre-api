package br.com.lumilivre.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import br.com.lumilivre.api.model.AppVersion;

@Repository
public interface AppVersionRepository extends JpaRepository<AppVersion, String> {
}
