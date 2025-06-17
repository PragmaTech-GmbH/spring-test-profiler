package digital.pragmatech.demo.repository;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

  Optional<Book> findByIsbn(String isbn);

  List<Book> findByAuthorContainingIgnoreCase(String author);

  List<Book> findByTitleContainingIgnoreCase(String title);

  List<Book> findByCategory(BookCategory category);

  List<Book> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

  @Query("SELECT b FROM Book b WHERE b.author = :author AND b.category = :category")
  List<Book> findByAuthorAndCategory(@Param("author") String author, @Param("category") BookCategory category);

  @Query("SELECT COUNT(b) FROM Book b WHERE b.category = :category")
  long countByCategory(@Param("category") BookCategory category);

  boolean existsByIsbn(String isbn);

  void deleteByIsbn(String isbn);
}
