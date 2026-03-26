package digital.pragmatech.demo.controller;

import java.util.List;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired
  private BookService bookService;

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
}
