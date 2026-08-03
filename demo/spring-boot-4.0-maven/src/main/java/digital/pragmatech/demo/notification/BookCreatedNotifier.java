package digital.pragmatech.demo.notification;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import digital.pragmatech.demo.service.BookCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
public class BookCreatedNotifier {

  private static final Logger LOG = LoggerFactory.getLogger(BookCreatedNotifier.class);

  private final List<String> notifiedIsbns = new CopyOnWriteArrayList<>();

  @ApplicationModuleListener
  void on(BookCreatedEvent event) {
    LOG.info("New book created: {} ({})", event.title(), event.isbn());
    notifiedIsbns.add(event.isbn());
  }

  public List<String> notifiedIsbns() {
    return List.copyOf(notifiedIsbns);
  }
}
