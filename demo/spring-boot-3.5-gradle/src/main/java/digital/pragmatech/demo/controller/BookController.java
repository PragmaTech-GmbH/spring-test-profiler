package digital.pragmatech.demo.controller;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.entity.BookCategory;
import digital.pragmatech.demo.service.BookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

  private final BookService bookService;

  public BookController(BookService bookService) {
    this.bookService = bookService;
  }

  @GetMapping
  public List<Book> getAllBooks() {
    return bookService.findAll();
  }

  @GetMapping("/{id}")
  public ResponseEntity<Book> getBookById(@PathVariable Long id) {
    return bookService.findById(id)
      .map(ResponseEntity::ok)
      .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/isbn/{isbn}")
  public ResponseEntity<Book> getBookByIsbn(@PathVariable String isbn) {
    return bookService.findByIsbn(isbn)
      .map(ResponseEntity::ok)
      .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/search")
  public List<Book> searchBooks(
    @RequestParam(required = false) String author,
    @RequestParam(required = false) String title,
    @RequestParam(required = false) BookCategory category,
    @RequestParam(required = false) BigDecimal minPrice,
    @RequestParam(required = false) BigDecimal maxPrice) {

    if (author != null) {
      return bookService.findByAuthor(author);
    }
    else if (title != null) {
      return bookService.findByTitle(title);
    }
    else if (category != null) {
      return bookService.findByCategory(category);
    }
    else if (minPrice != null && maxPrice != null) {
      return bookService.findByPriceRange(minPrice, maxPrice);
    }
    else {
      return bookService.findAll();
    }
  }

  @PostMapping
  public ResponseEntity<Book> createBook(@RequestBody Book book) {
    try {
      Book createdBook = bookService.createBook(book);
      return ResponseEntity.status(HttpStatus.CREATED).body(createdBook);
    }
    catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<Book> updateBook(@PathVariable Long id, @RequestBody Book book) {
    try {
      Book updatedBook = bookService.updateBook(id, book);
      return ResponseEntity.ok(updatedBook);
    }
    catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
    try {
      bookService.deleteBook(id);
      return ResponseEntity.noContent().build();
    }
    catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/isbn/{isbn}")
  public ResponseEntity<Void> deleteBookByIsbn(@PathVariable String isbn) {
    try {
      bookService.deleteByIsbn(isbn);
      return ResponseEntity.noContent().build();
    }
    catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/count")
  public ResponseEntity<Long> getBookCount() {
    return ResponseEntity.ok(bookService.countBooks());
  }

  @GetMapping("/count/{category}")
  public ResponseEntity<Long> getBookCountByCategory(@PathVariable BookCategory category) {
    return ResponseEntity.ok(bookService.countByCategory(category));
  }
}
