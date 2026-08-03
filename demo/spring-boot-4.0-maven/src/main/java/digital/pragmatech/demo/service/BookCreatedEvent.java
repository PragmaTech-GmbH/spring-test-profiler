package digital.pragmatech.demo.service;

public record BookCreatedEvent(Long bookId, String isbn, String title) {}
