package br.com.lumilivre.api.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import br.com.lumilivre.api.model.ExemplarModel;

@Repository
public interface ExemplarRepository extends JpaRepository<ExemplarModel, String>{
	Optional<ExemplarModel> findByLivroIsbn(String isbn);
	Optional<ExemplarModel> findByTombo(String tombo);
}
