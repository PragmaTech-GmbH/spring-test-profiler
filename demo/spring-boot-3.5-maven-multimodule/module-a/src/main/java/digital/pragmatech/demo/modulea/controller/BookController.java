package digital.pragmatech.demo.modulea.controller;

import java.util.List;

import digital.pragmatech.demo.modulea.entity.Book;
import digital.pragmatech.demo.modulea.service.BookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BookController {

  private final BookService bookService;

  public BookController(BookService bookService) {
    this.bookService = bookService;
  }

  @PostMapping
  public ResponseEntity<Book> createBook(@RequestBody Book book) {
    Book createdBook = bookService.createBook(book);
    return new ResponseEntity<>(createdBook, HttpStatus.CREATED);
  }

  @GetMapping
  public ResponseEntity<List<Book>> getAllBooks() {
    List<Book> books = bookService.findAll();
    return ResponseEntity.ok(books);
  }

  @GetMapping("/count")
  public ResponseEntity<Long> getBookCount() {
    long count = bookService.count();
    return ResponseEntity.ok(count);
  }
}
