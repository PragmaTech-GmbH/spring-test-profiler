package digital.pragmatech.demo.moduleb.repository;

import java.util.List;

import digital.pragmatech.demo.moduleb.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {

  List<Author> findByNameContainingIgnoreCase(String name);

  boolean existsByEmail(String email);
}
