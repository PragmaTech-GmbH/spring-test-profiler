package digital.pragmatech.demo.integration;

import digital.pragmatech.demo.model.User;
import digital.pragmatech.demo.repository.UserRepository;
import digital.pragmatech.springtestinsight.SpringTestInsightExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith({SpringExtension.class, SpringTestInsightExtension.class})
class UserIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }
    
    @Test
    void testCreateAndRetrieveUser() {
        // Create user
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", "integration_user");
        params.add("email", "integration@example.com");
        
        ResponseEntity<User> createResponse = restTemplate.postForEntity(
            "/api/users", 
            params, 
            User.class
        );
        
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        Long userId = createResponse.getBody().getId();
        
        // Retrieve user
        ResponseEntity<User> getResponse = restTemplate.getForEntity(
            "/api/users/" + userId,
            User.class
        );
        
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("integration_user", getResponse.getBody().getUsername());
        assertEquals("integration@example.com", getResponse.getBody().getEmail());
    }
    
    @Test
    void testGetAllUsers() {
        // Create multiple users
        userRepository.save(new User("user1", "user1@example.com"));
        userRepository.save(new User("user2", "user2@example.com"));
        userRepository.save(new User("user3", "user3@example.com"));
        
        ResponseEntity<User[]> response = restTemplate.getForEntity(
            "/api/users",
            User[].class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().length);
    }
    
    @Test
    void testDeleteUser() {
        // Create a user first
        User user = userRepository.save(new User("to_delete", "delete@example.com"));
        
        // Delete the user
        restTemplate.delete("/api/users/" + user.getId());
        
        // Verify deletion
        assertFalse(userRepository.findById(user.getId()).isPresent());
    }
    
    @Test
    void testCreateUserWithDuplicateUsername() {
        // Create first user
        userRepository.save(new User("duplicate", "first@example.com"));
        
        // Try to create another user with same username
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("username", "duplicate");
        params.add("email", "second@example.com");
        
        ResponseEntity<User> response = restTemplate.postForEntity(
            "/api/users",
            params,
            User.class
        );
        
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}