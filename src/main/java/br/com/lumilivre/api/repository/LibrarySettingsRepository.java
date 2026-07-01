package br.com.lumilivre.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.lumilivre.api.model.LibrarySettings;

public interface LibrarySettingsRepository extends JpaRepository<LibrarySettings, Boolean> {
}
