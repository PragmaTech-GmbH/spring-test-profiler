package digital.pragmatech.demo.controller;

import digital.pragmatech.demo.entity.Book;
import digital.pragmatech.demo.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<Book[]> getAllBooks() {
        List<Book> books = bookService.findAll();
        return ResponseEntity.ok(books.toArray(new Book[0]));
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getBookCount() {
        long count = bookService.count();
        return ResponseEntity.ok(count);
    }
}
