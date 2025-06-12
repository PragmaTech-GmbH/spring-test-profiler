package digital.pragmatech.demo.repository;

import digital.pragmatech.demo.model.User;
import digital.pragmatech.springtestinsight.SpringTestInsightExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ExtendWith({SpringExtension.class, SpringTestInsightExtension.class})
class UserRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }
    
    @Test
    void testSaveUser() {
        User user = new User("john_doe", "john@example.com");
        
        User savedUser = userRepository.save(user);
        
        assertNotNull(savedUser.getId());
        assertEquals("john_doe", savedUser.getUsername());
        assertEquals("john@example.com", savedUser.getEmail());
    }
    
    @Test
    void testFindByUsername() {
        User user = new User("jane_doe", "jane@example.com");
        userRepository.save(user);
        
        Optional<User> found = userRepository.findByUsername("jane_doe");
        
        assertTrue(found.isPresent());
        assertEquals("jane_doe", found.get().getUsername());
        assertEquals("jane@example.com", found.get().getEmail());
    }
    
    @Test
    void testFindByEmail() {
        User user = new User("bob_smith", "bob@example.com");
        userRepository.save(user);
        
        Optional<User> found = userRepository.findByEmail("bob@example.com");
        
        assertTrue(found.isPresent());
        assertEquals("bob_smith", found.get().getUsername());
        assertEquals("bob@example.com", found.get().getEmail());
    }
    
    @Test
    void testExistsByUsername() {
        User user = new User("alice", "alice@example.com");
        userRepository.save(user);
        
        assertTrue(userRepository.existsByUsername("alice"));
        assertFalse(userRepository.existsByUsername("charlie"));
    }
    
    @Test
    void testExistsByEmail() {
        User user = new User("david", "david@example.com");
        userRepository.save(user);
        
        assertTrue(userRepository.existsByEmail("david@example.com"));
        assertFalse(userRepository.existsByEmail("eve@example.com"));
    }
    
    @Test
    void testDeleteUser() {
        User user = new User("frank", "frank@example.com");
        User savedUser = userRepository.save(user);
        
        userRepository.deleteById(savedUser.getId());
        
        assertFalse(userRepository.findById(savedUser.getId()).isPresent());
    }
}