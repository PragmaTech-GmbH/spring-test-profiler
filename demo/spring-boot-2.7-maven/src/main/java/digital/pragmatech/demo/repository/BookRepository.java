package digital.pragmatech.demo.repository;

import java.util.List;
import java.util.Optional;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

  Optional<Book> findByIsbn(String isbn);

  List<Book> findByAuthorContainingIgnoreCase(String author);

  List<Book> findByCategory(BookCategory category);

  @Query("SELECT COUNT(b) FROM Book b WHERE b.category = :category")
  long countByCategory(@Param("category") BookCategory category);

  boolean existsByIsbn(String isbn);
}
