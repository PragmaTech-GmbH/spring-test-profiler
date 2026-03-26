package digital.pragmatech.demo.modulea.repository;

import java.util.List;
import java.util.Optional;

import digital.pragmatech.demo.modulea.entity.Book;
import digital.pragmatech.demo.modulea.entity.BookCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

  Optional<Book> findByIsbn(String isbn);

  List<Book> findByCategory(BookCategory category);

  boolean existsByIsbn(String isbn);
}
