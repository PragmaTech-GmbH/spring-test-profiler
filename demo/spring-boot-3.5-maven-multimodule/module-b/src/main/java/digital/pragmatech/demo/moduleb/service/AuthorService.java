package digital.pragmatech.demo.moduleb.service;

import java.util.List;

import digital.pragmatech.demo.moduleb.entity.Author;
import digital.pragmatech.demo.moduleb.repository.AuthorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthorService {

  private final AuthorRepository authorRepository;

  public AuthorService(AuthorRepository authorRepository) {
    this.authorRepository = authorRepository;
  }

  public Author createAuthor(Author author) {
    if (authorRepository.existsByEmail(author.getEmail())) {
      throw new IllegalArgumentException("Author with email " + author.getEmail() + " already exists");
    }
    return authorRepository.save(author);
  }

  @Transactional(readOnly = true)
  public List<Author> findAll() {
    return authorRepository.findAll();
  }

  @Transactional(readOnly = true)
  public List<Author> findByName(String name) {
    return authorRepository.findByNameContainingIgnoreCase(name);
  }

  @Transactional(readOnly = true)
  public long count() {
    return authorRepository.count();
  }
}
